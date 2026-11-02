package dev.abyssace.kumo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.BriefTemplateManager;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.config.StoreType;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import dev.abyssace.kumo.fake.BriefFixtures;
import dev.abyssace.kumo.fake.FakeOllamaServer;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.JsonVectorStore;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class TaskCommandTest {
    @TempDir Path directory;
    private FakeOllamaServer server;
    private Path home;
    private Path corpus;
    private Path file;
    private String previousHome;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeOllamaServer().chatResponse(BriefFixtures.valid().toString());
        home = directory.resolve("home");
        corpus = Files.createDirectories(directory.resolve("corpus"));
        file = corpus.resolve("prueba.txt");
        Files.writeString(file, "Kumo es un asistente local. Debe instalarse mediante un script.");
        previousHome = System.getProperty(ConfigManager.KUMO_HOME_PROPERTY);
        System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, home.toString());
        new ConfigManager(home).save(AppConfig.builder().ollamaBaseUrl(server.baseUrl())
                .numCtx(8192).chatModel("modelo-configurado").vectorStore(StoreType.JSON).build());
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
    void briefEnviaFormatComoEsquemaJsonYNoNecesitaIndiceNiEmbeddings() throws Exception {
        Result result = run("brief", file.toString());
        assertEquals(0, result.code(), result.err());
        var json = mapper.readTree(result.out());
        assertEquals(file.toString(), json.path("source").asText());
        assertEquals(BriefFixtures.valid(), json.get("brief"));
        assertTrue(result.err().contains(Messages.BRIEF_EXTRACT));
        assertTrue(result.err().contains(Messages.BRIEF_VALIDATE));
        assertTrue(Files.exists(home.resolve(BriefTemplateManager.FILE_NAME)));
        assertFalse(Files.exists(JsonVectorStore.indexPath(corpus)));
        assertEquals(1, server.requests().size());
        var request = mapper.readTree(server.lastRequest("/api/chat").orElseThrow().body());
        assertTrue(request.path("format").isObject());
        assertEquals("object", request.path("format").path("type").asText());
        assertTrue(request.path("format").path("properties").has("tasks"));
        assertEquals(8192, request.path("options").path("num_ctx").asInt());
        assertEquals("modelo-configurado", request.path("model").asText());
        assertFalse(request.path("think").asBoolean(true));
        assertFalse(request.path("stream").asBoolean(true));
    }

    @Test
    void briefCarpetaProcesaArchivoPorArchivoSinMezclarFuentes() throws Exception {
        server.chatResponder(request -> {
            var value = BriefFixtures.valid();
            var choices = request.path("format").path("properties").path("documentType").path("enum");
            if (choices.size() == 1 && choices.get(0).asText().equals("code")) {
                value.put("documentType", "code");
            }
            return value.toString();
        });
        Path code = corpus.resolve("a.py");
        Files.writeString(code, "print('hola')");
        Files.writeString(Files.createDirectories(corpus.resolve(".kumo")).resolve("ignorado.txt"), "ignorar");
        Result result = run("brief", corpus.toString(), "--language", "English");
        assertEquals(0, result.code(), result.err());
        var results = mapper.readTree(result.out()).path("results");
        assertEquals(2, results.size());
        assertEquals(code.toString(), results.get(0).path("source").asText());
        assertEquals(file.toString(), results.get(1).path("source").asText());
        assertEquals(2, server.countRequests("/api/chat"));
        for (var request : server.requests()) {
            assertEquals("/api/chat", request.path());
            String prompt = mapper.readTree(request.body()).path("messages").get(0).path("content").asText();
            assertTrue(prompt.contains("Write the values in English"));
        }
    }

    @Test
    void briefUsaUnaPlantillaEditableSinAlterarla() throws Exception {
        Path template = directory.resolve("plantilla.json");
        String text = "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"Usa dos frases.\"}";
        Files.writeString(template, text);
        server.chatResponse("{\"title\":\"kumo\",\"summary\":\"Resumen breve.\"}");
        Result result = run("brief", file.toString(), "--template", template.toString(), "--language", "español");
        assertEquals(0, result.code(), result.err());
        assertEquals(2, mapper.readTree(result.out()).path("brief").size());
        var request = mapper.readTree(server.lastRequest("/api/chat").orElseThrow().body());
        assertEquals(2, request.path("format").path("properties").size());
        assertTrue(request.path("messages").get(0).path("content").asText().contains("Usa dos frases."));
        assertEquals(text, Files.readString(template));
    }

    @Test
    void briefLargoResumeAntesDeExtraerYEnviaContextoEnCadaPeticion() throws Exception {
        Files.writeString(file, "contenido importante ".repeat(3000));
        server.chatResponder(request -> request.has("format")
                ? BriefFixtures.valid().toString() : "Síntesis breve de la fuente.");
        Result result = run("brief", file.toString());
        assertEquals(0, result.code(), result.err());
        assertTrue(mapper.readTree(result.out()).path("compressed").asBoolean());
        assertTrue(result.err().contains(Messages.BRIEF_COMPRESSION));
        assertTrue(server.countRequests("/api/chat") > 2);
        for (var request : server.requests()) {
            var body = mapper.readTree(request.body());
            assertEquals(8192, body.path("options").path("num_ctx").asInt());
            assertFalse(body.path("stream").asBoolean(true));
            assertFalse(body.path("think").asBoolean(true));
        }
    }

    @Test
    void briefGuardaJsonValidoSinSobrescribir() throws Exception {
        Path output = directory.resolve("ficha.json");
        Result first = run("brief", file.toString(), "--output", output.toString());
        assertEquals(0, first.code(), first.err());
        assertEquals(mapper.readTree(first.out()), mapper.readTree(Files.readString(output)));
        int before = server.requests().size();
        assertEquals(1, run("brief", file.toString(), "--output", output.toString()).code());
        assertEquals(before, server.requests().size());
        assertEquals(first.out(), Files.readString(output));
    }

    @Test
    void briefRechazaJsonInvalidoYNoPublicaUnLoteParcial() throws Exception {
        Files.writeString(corpus.resolve("segundo.txt"), "otro documento");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        server.chatResponder(request -> calls.incrementAndGet() == 1 ? BriefFixtures.valid().toString() : "{ roto");
        Path output = directory.resolve("fichas.json");
        Result result = run("brief", corpus.toString(), "--output", output.toString());
        assertEquals(1, result.code());
        assertEquals(2, server.countRequests("/api/chat"));
        assertTrue(result.out().isEmpty());
        assertFalse(Files.exists(output));
        assertTrue(result.err().contains("JSON válida"));
    }

    @Test
    void briefRechazaPlantillaInvalidaAntesDeLlamarAlModelo() throws Exception {
        Path template = directory.resolve("mala.json");
        Files.writeString(template, "{}");
        assertEquals(1, run("brief", file.toString(), "--template", template.toString()).code());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void writeGeneraMarkdownConFuentesYUsaWriteTopKSinModificarElIndice() throws Exception {
        seedIndex(14);
        byte[] before = Files.readAllBytes(JsonVectorStore.indexPath(corpus));
        server.chatResponse("## Introducción\nKumo es local [1].");
        Path output = directory.resolve("borrador.md");
        Result result = run("write", "--corpus", corpus.toString(), "Redacta una introducción",
                "--output", output.toString(), "--verbose");
        assertEquals(0, result.code(), result.err());
        assertEquals(result.out(), Files.readString(output));
        assertTrue(result.out().contains(Messages.WRITE_SOURCES));
        assertTrue(result.out().contains("[12]"));
        assertFalse(result.out().contains("[13]"));
        assertTrue(result.out().contains("similitud"));
        assertTrue(result.err().contains(Messages.WRITE_RETRIEVE));
        assertTrue(result.err().contains(Messages.WRITE_GENERATE));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(JsonVectorStore.indexPath(corpus))));
        assertEquals(1, server.countRequests("/api/embed"));
        var request = mapper.readTree(server.lastRequest("/api/chat").orElseThrow().body());
        assertFalse(request.has("format"));
        assertFalse(request.path("stream").asBoolean(true));
        assertEquals(8192, request.path("options").path("num_ctx").asInt());
    }

    @Test
    void writeAceptaIdiomaYTopKExplicitos() {
        seedIndex(4);
        server.chatResponse("Introduction [1].");
        Result result = run("write", "--corpus", corpus.toString(), "Introduce Kumo",
                "--top-k", "2", "--language", "English");
        assertEquals(0, result.code(), result.err());
        assertTrue(result.out().contains("Retrieved sources"));
        assertFalse(result.out().contains("[3]"));
        assertTrue(server.lastRequest("/api/chat").orElseThrow().body().contains("Write in English"));
    }

    @Test
    void writeSinIndiceNoLlamaAOllama() {
        Result result = run("write", "--corpus", corpus.toString(), "Redacta");
        assertEquals(1, result.code());
        assertTrue(result.err().contains(Messages.INDEX_EMPTY));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void writeRechazaSalidasVaciasSinCitasOConFuentesInventadas() {
        seedIndex(1);
        for (String text : List.of("", "sin citas", "texto [77]")) {
            server.chatResponse(text);
            Path output = directory.resolve("fallido.md");
            Result result = run("write", "--corpus", corpus.toString(), "Redacta", "--output", output.toString());
            assertEquals(1, result.code());
            assertTrue(result.out().isEmpty());
            assertFalse(Files.exists(output));
        }
    }

    @Test
    void writeNoSobrescribeUnArchivoNiGeneraAntesDeValidarLaSalida() throws Exception {
        Path output = directory.resolve("usuario.md");
        Files.writeString(output, "contenido del usuario");
        Result result = run("write", "--corpus", corpus.toString(), "Redacta", "--output", output.toString());
        assertEquals(1, result.code());
        assertEquals("contenido del usuario", Files.readString(output));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void erroresHttpNoPublicanFichasNiBorradores() {
        server.chatStatus(500);
        Result brief = run("brief", file.toString());
        assertEquals(1, brief.code());
        assertTrue(brief.out().isEmpty());
        seedIndex(1);
        Result write = run("write", "--corpus", corpus.toString(), "Redacta");
        assertEquals(1, write.code());
        assertTrue(write.out().isEmpty());
    }

    @Test
    void validaArgumentosYOfreceAyudaSinLlamarAOllama() {
        assertEquals(0, run("brief", "--help").code());
        assertEquals(0, run("write", "--help").code());
        assertEquals(2, run("brief").code());
        assertEquals(2, run("write").code());
        assertEquals(1, run("brief", file.toString(), "--language", "fr").code());
        assertEquals(1, run("write", "--corpus", corpus.toString(), "  ").code());
        assertEquals(1, run("write", "--corpus", corpus.toString(), "Redacta", "--top-k", "0").code());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void initCreaLaPlantillaPeroNoReemplazaUnaExistente() throws Exception {
        assertEquals(0, run("init").code());
        Path template = home.resolve(BriefTemplateManager.FILE_NAME);
        assertTrue(Files.exists(template));
        String custom = "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"Mi preferencia.\"}";
        Files.writeString(template, custom);
        assertEquals(0, run("init").code());
        assertEquals(custom, Files.readString(template));
    }

    private void seedIndex(int count) {
        var store = new JsonVectorStore(JsonVectorStore.indexPath(corpus), new CosineSimilarity(), "embeddinggemma");
        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new EmbeddedChunk(Chunk.of(file, i, "Kumo local " + i,
                    new SourceLocation.LineRange(i + 1, i + 1)), new float[] {1, 0, 0}));
        }
        store.add(chunks);
    }

    private Result run(String... arguments) {
        var cli = Main.createCommandLine();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        redirect(cli, new PrintWriter(out, true), new PrintWriter(err, true));
        return new Result(cli.execute(arguments), out.toString(), err.toString());
    }

    private void redirect(CommandLine cli, PrintWriter out, PrintWriter err) {
        cli.setOut(out);
        cli.setErr(err);
        cli.getSubcommands().values().forEach(child -> redirect(child, out, err));
    }

    private record Result(int code, String out, String err) { }
}
