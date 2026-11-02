package dev.abyssace.kumo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.fake.FakeOllamaServer;
import dev.abyssace.kumo.i18n.Messages;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class SummarizeCommandTest {
    @TempDir Path directory;
    private FakeOllamaServer server;
    private Path file;
    private String previousHome;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeOllamaServer().chatResponse("## Idea principal\nResumen fiel.");
        Path home = directory.resolve("config");
        previousHome = System.getProperty(ConfigManager.KUMO_HOME_PROPERTY);
        System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, home.toString());
        new ConfigManager(home).save(AppConfig.builder().ollamaBaseUrl(server.baseUrl())
                .chatModel("modelo-configurado").numCtx(4096).summaryInputRatio(0.3).build());
        file = directory.resolve("documento.txt");
        Files.writeString(file, "Información importante del documento.");
    }

    @AfterEach
    void tearDown() {
        server.close();
        if (previousHome == null) {
            System.clearProperty(ConfigManager.KUMO_HOME_PROPERTY);
        } else {
            System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, previousHome);
        }
    }

    @Test
    void resumeSinIndiceNiEmbeddingsYSeparaElProgreso() throws Exception {
        Result result = run(file.toString());
        assertEquals(0, result.code(), result.err());
        assertEquals("## Idea principal\nResumen fiel." + System.lineSeparator(), result.out());
        assertTrue(result.err().contains("Estrategia: stuff"));
        assertTrue(result.err().contains(Messages.SUMMARY_FITS));
        assertTrue(result.err().contains(Messages.SUMMARY_FINAL));
        assertFalse(Files.exists(directory.resolve(".kumo")));
        assertEquals(1, server.requests().size());
        var body = new ObjectMapper().readTree(server.lastRequest("/api/chat").orElseThrow().body());
        assertEquals(4096, body.path("options").path("num_ctx").asInt());
        assertEquals("modelo-configurado", body.path("model").asText());
        assertFalse(body.path("think").asBoolean(true));
        assertFalse(body.path("stream").asBoolean(true));
    }

    @Test
    void todasLasPeticionesMapReduceEnvianContextoEIdioma() throws Exception {
        Files.writeString(file, "Datos del documento. ".repeat(4000));
        server.chatResponder(request -> {
            String prompt = request.path("messages").get(0).path("content").asText();
            String marker = "CONTENIDO A RESUMIR (hasta el final del mensaje):\n";
            String content = prompt.substring(prompt.indexOf(marker) + marker.length());
            return "r".repeat(Math.max(1, content.length() / 2));
        });
        Result result = run(file.toString(), "--language", "english");
        assertEquals(0, result.code(), result.err());
        assertTrue(result.err().contains("Estrategia: map-reduce"));
        assertTrue(result.err().contains(Messages.SUMMARY_MAP));
        assertTrue(result.err().contains(Messages.SUMMARY_REDUCE.formatted(2)));
        assertTrue(server.requests().size() > 2);
        for (var request : server.requests()) {
            assertEquals("/api/chat", request.path());
            var body = new ObjectMapper().readTree(request.body());
            assertEquals(4096, body.path("options").path("num_ctx").asInt());
            assertTrue(body.path("messages").get(0).path("content").asText().contains("Respond in English"));
            assertFalse(body.path("think").asBoolean(true));
        }
    }

    @Test
    void unFalloTrasResumenesParcialesNoPublicaUnaSalidaIncompleta() throws Exception {
        Files.writeString(file, "contenido ".repeat(3000));
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        server.chatResponder(request -> calls.incrementAndGet() == 1 ? "parcial correcto" : "");
        Path target = directory.resolve("incompleto.md");
        Result result = run(file.toString(), "--output", target.toString());
        assertEquals(1, result.code());
        assertEquals(2, server.countRequests("/api/chat"));
        assertTrue(result.out().isEmpty());
        assertFalse(Files.exists(target));
    }

    @Test
    void guardaSoloMarkdownSinSobrescribir() throws Exception {
        Path target = directory.resolve("resumen.md");
        Result first = run(file.toString(), "--output", target.toString());
        assertEquals(0, first.code(), first.err());
        assertEquals(first.out(), Files.readString(target));
        int before = server.requests().size();
        Result second = run(file.toString(), "--output", target.toString());
        assertEquals(1, second.code());
        assertTrue(second.err().contains("ya existe"));
        assertEquals(first.out(), Files.readString(target));
        assertEquals(before, server.requests().size());
    }

    @Test
    void noPermiteUsarLaFuenteComoSalida() throws Exception {
        String before = Files.readString(file);
        assertEquals(1, run(file.toString(), "--output", file.toString()).code());
        assertEquals(before, Files.readString(file));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void noGuardaResultadosVaciosNiParcialesTrasUnError() {
        Path target = directory.resolve("fallido.md");
        server.chatResponse("  ");
        Result empty = run(file.toString(), "--output", target.toString());
        assertEquals(1, empty.code());
        assertTrue(empty.err().contains(Messages.SUMMARY_EMPTY));
        assertFalse(Files.exists(target));
        server.chatStatus(500);
        Result failed = run(file.toString(), "--output", target.toString());
        assertEquals(1, failed.code());
        assertTrue(failed.out().isEmpty());
        assertFalse(Files.exists(target));
    }

    @Test
    void validaIdiomaYEstrategiaAntesDeLlamarAOllama() {
        assertEquals(1, run(file.toString(), "--language", "fr").code());
        assertEquals(1, run(file.toString(), "--strategy", "inventada").code());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void rechazaStuffDemasiadoGrandeSinTruncar() throws Exception {
        Files.writeString(file, "x".repeat(20000));
        Result result = run(file.toString(), "--strategy", "stuff");
        assertEquals(1, result.code());
        assertTrue(result.err().contains("no se ha truncado"));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void admiteCodigoYEspanolConTildes() throws Exception {
        Path code = directory.resolve("Main.java");
        Files.writeString(code, "class Main { public static void main(String[] args) {} }");
        Result result = run(code.toString(), "--language", "ESPAÑOL");
        assertEquals(0, result.code(), result.err());
        assertTrue(result.err().contains(Messages.SUMMARY_CODE_WARNING));
        assertTrue(server.lastRequest("/api/chat").orElseThrow().body().contains("Responde en español"));
    }

    @Test
    void rechazaCarpetasArchivosAusentesYFormatosNoSoportados() {
        assertEquals(1, run(directory.toString()).code());
        assertEquals(1, run(directory.resolve("ausente.txt").toString()).code());
        assertEquals(1, run(directory.resolve("datos.bin").toString()).code());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void salidaConCarpetaInexistenteFallaAntesDeGenerar() {
        Result result = run(file.toString(), "--output", directory.resolve("ausente/out.md").toString());
        assertEquals(1, result.code());
        assertTrue(result.err().contains("carpeta de destino"));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void ayudaEnEspanolYArchivoObligatorio() {
        Result help = run("--help");
        assertEquals(0, help.code());
        assertTrue(help.out().contains("--strategy"));
        assertTrue(help.out().contains("--language"));
        assertTrue(help.out().contains("Parámetros:"));
        assertEquals(2, run().code());
    }

    private Result run(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "summarize";
        System.arraycopy(args, 0, command, 1, args.length);
        var cli = Main.createCommandLine();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        redirect(cli, new PrintWriter(out, true), new PrintWriter(err, true));
        return new Result(cli.execute(command), out.toString(), err.toString());
    }

    private void redirect(CommandLine cli, PrintWriter out, PrintWriter err) {
        cli.setOut(out);
        cli.setErr(err);
        cli.getSubcommands().values().forEach(child -> redirect(child, out, err));
    }

    private record Result(int code, String out, String err) { }
}
