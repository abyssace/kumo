package dev.abyssace.kumo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.config.StoreType;
import dev.abyssace.kumo.fake.FakeOllamaServer;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.store.JsonVectorStore;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class CliCommandTest {

    @TempDir
    Path tempDir;

    private FakeOllamaServer server;
    private Path home;
    private Path corpus;
    private String previousHome;

    @BeforeEach
    void setUp() throws IOException {
        server = new FakeOllamaServer();
        home = tempDir.resolve("home");
        corpus = Files.createDirectories(tempDir.resolve("corpus"));
        previousHome = System.getProperty(ConfigManager.KUMO_HOME_PROPERTY);
        System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, home.toString());
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
    void ayudaMuestraLosEncabezadosEnEspanol() {
        Result result = run("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains(Messages.CLI_USAGE_HEADING));
        assertTrue(result.out().contains(Messages.CLI_DESCRIPTION_HEADING));
        assertTrue(result.out().contains(Messages.CLI_OPTIONS_HEADING));
        assertTrue(result.out().contains(Messages.CLI_COMMANDS_HEADING));
        assertFalse(result.out().contains("Usage:"), "La ayuda no debe mezclar inglés");
        assertFalse(result.out().contains("Options:"));
        assertFalse(result.out().contains("Commands:"));
    }

    @Test
    void versionMuestraElNombreDelPrograma() {
        Result result = run("--version");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("kumo"));
    }

    @Test
    void unaOpcionDesconocidaSeTraduceAlEspanol() {
        Result result = run("--no-existe");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains(Messages.CLI_UNKNOWN_ARGUMENT.formatted("--no-existe")));
        assertFalse(result.err().contains("Unknown option"));
    }

    @Test
    void askSinPreguntaInformaDelParametroFaltante() {
        Result result = run("ask");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains(Messages.CLI_MISSING_PARAMETER.formatted("<pregunta>")));
    }

    @Test
    void initCreaLaConfiguracionGlobal() {
        Result result = run("init");

        assertEquals(0, result.exitCode());
        Path configFile = home.resolve(ConfigManager.CONFIG_FILE_NAME);
        assertTrue(Files.isRegularFile(configFile));
        assertTrue(result.out().contains(Messages.CLI_INIT_CREATED.formatted(
                configFile.toAbsolutePath().normalize())));
    }

    @Test
    void initReutilizaLaConfiguracionExistente() throws IOException {
        writeConfig();

        Result result = run("init");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains(Messages.CLI_INIT_FOUND.formatted(
                home.resolve(ConfigManager.CONFIG_FILE_NAME).toAbsolutePath().normalize())));
        assertTrue(result.out().contains(Messages.OLLAMA_CONNECTED.formatted(server.baseUrl())));
        assertTrue(result.out().contains(Messages.OLLAMA_MODEL_INSTALLED));
    }

    @Test
    void initConOllamaCaidoAvisaPeroSaleCero() throws IOException {
        writeConfig();
        server.tagsStatus(500);

        Result result = run("init");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains(Messages.CLI_INIT_MODELS_HINT));
        assertTrue(result.out().contains(Messages.CLI_INIT_HINT));
    }

    @Test
    void addIndexaYAskRespondeConFuentes() throws IOException {
        writeConfig();
        Path file = corpus.resolve("notas.txt");
        Files.writeString(file, "El gato duerme en el sofá. El perro ladra en el jardín.",
                StandardCharsets.UTF_8);

        Result add = run("add", "--corpus", corpus.toString(), file.toString());

        assertEquals(0, add.exitCode(), add.err());
        Path index = JsonVectorStore.indexPath(corpus);
        assertTrue(Files.isRegularFile(index));
        assertTrue(add.out().contains(Messages.INDEXING_SUMMARY.formatted(1, 1, 0)));

        Result ask = run("ask", "--corpus", corpus.toString(), "¿Dónde duerme el gato?");

        assertEquals(0, ask.exitCode(), ask.err());
        assertTrue(ask.out().contains("respuesta del modelo"));
        assertTrue(ask.out().contains(Messages.SOURCES_HEADER));
        assertTrue(ask.out().contains("notas.txt"));
    }

    @Test
    void addConRutaInexistenteDevuelveUno() throws IOException {
        writeConfig();
        Path missing = corpus.resolve("no-existe.txt");

        Result result = run("add", "--corpus", corpus.toString(), missing.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains(Messages.FILE_NOT_FOUND.formatted(
                missing.toAbsolutePath().normalize())));
    }

    @Test
    void addSinModeloDeEmbeddingsFallaConInstruccion() throws IOException {
        writeConfig();
        server.modelNames(java.util.List.of("qwen3:8b"));
        Path file = corpus.resolve("notas.txt");
        Files.writeString(file, "texto", StandardCharsets.UTF_8);

        Result result = run("add", "--corpus", corpus.toString(), file.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains(Messages.OLLAMA_MODEL_NOT_INSTALLED.formatted(
                AppConfig.DEFAULT_EMBEDDING_MODEL, AppConfig.DEFAULT_EMBEDDING_MODEL)));
    }

    @Test
    void addConContextoDemasiadoPequenoFalla() throws IOException {
        writeConfig();
        server.showContextLength(100);
        Path file = corpus.resolve("notas.txt");
        Files.writeString(file, "texto", StandardCharsets.UTF_8);

        Result result = run("add", "--corpus", corpus.toString(), file.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("contexto de 100 tokens"));
        assertTrue(result.err().contains(String.valueOf(AppConfig.DEFAULT_CHUNK_SIZE_TOKENS)));
    }

    @Test
    void addConUnArchivoNoSoportadoCuentaComoFallo() throws IOException {
        writeConfig();
        Path file = corpus.resolve("datos.bin");
        Files.writeString(file, "binario", StandardCharsets.UTF_8);

        Result result = run("add", "--corpus", corpus.toString(), file.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.out().contains("Formato no soportado"));
        assertTrue(result.out().contains(Messages.INDEXING_SUMMARY.formatted(0, 0, 1)));
    }

    @Test
    void addConUnaCarpetaVaciaAvisaDeQueNoHayArchivos() throws IOException {
        writeConfig();

        Result result = run("add", "--corpus", corpus.toString(), corpus.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("No encontré archivos compatibles"));
    }

    @Test
    void addAceptaUnaCarpetaYDetectaArchivos() throws IOException {
        writeConfig();
        Files.writeString(corpus.resolve("uno.txt"), "primer documento", StandardCharsets.UTF_8);
        Files.writeString(corpus.resolve("dos.md"), "# segundo documento", StandardCharsets.UTF_8);

        Result result = run("add", "--corpus", corpus.toString(), corpus.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains(Messages.INDEXING_SUMMARY.formatted(2, 2, 0)));
    }

    @Test
    void askConIndiceVacioDevuelveUno() throws IOException {
        writeConfig();

        Result result = run("ask", "--corpus", corpus.toString(), "¿algo?");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains(Messages.INDEX_EMPTY));
    }

    @Test
    void askConPreguntaEnBlancoDevuelveUno() throws IOException {
        writeConfig();

        Result result = run("ask", "--corpus", corpus.toString(), "   ");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains(Messages.QUESTION_EMPTY));
    }

    @Test
    void askConTopKInvalidoFalla() throws IOException {
        writeConfig();

        Result result = run("ask", "--corpus", corpus.toString(), "--top-k", "0", "¿algo?");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains(Messages.TOP_K_INVALID.formatted(0)));
    }

    @Test
    void statusMuestraConfiguracionOllamaEIndice() throws IOException {
        writeConfig();

        Result result = run("status", "--corpus", corpus.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains(Messages.STATUS_CONFIG.formatted(
                home.resolve(ConfigManager.CONFIG_FILE_NAME).toAbsolutePath().normalize())));
        assertTrue(result.out().contains(Messages.STATUS_CORPUS.formatted(corpus)));
        assertTrue(result.out().contains(Messages.STATUS_INDEX_EMPTY));
        assertTrue(result.out().contains(Messages.STATUS_REACHABLE.formatted(server.baseUrl())));
    }

    @Test
    void statusConOllamaCaidoNoFalla() throws IOException {
        writeConfig();
        server.tagsStatus(500);

        Result result = run("status", "--corpus", corpus.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains("no disponible"));
        assertTrue(result.out().contains(Messages.STATUS_UNKNOWN));
    }

    @Test
    void statusReportaUnIndiceIlegibleSinFallar() throws IOException {
        writeConfig();
        Path index = JsonVectorStore.indexPath(corpus);
        Files.createDirectories(index.getParent());
        Files.writeString(index, "{ roto", StandardCharsets.UTF_8);

        Result result = run("status", "--corpus", corpus.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains("ilegible"));
    }

    @Test
    void statusReportaElIndiceYSuModelo() throws IOException {
        writeConfig();
        Path file = corpus.resolve("notas.txt");
        Files.writeString(file, "contenido", StandardCharsets.UTF_8);
        run("add", "--corpus", corpus.toString(), file.toString());

        Result result = run("status", "--corpus", corpus.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains(Messages.STATUS_INDEX_DETAILS.formatted(
                1, 3, AppConfig.DEFAULT_EMBEDDING_MODEL)));
    }

    @Test
    void statusConCorpusInexistenteNoFalla() throws IOException {
        writeConfig();
        Path missing = tempDir.resolve("no-existe");

        Result result = run("status", "--corpus", missing.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains(Messages.STATUS_CORPUS.formatted(
                missing.toAbsolutePath().normalize())));
    }

    @Test
    void unaConfiguracionCorruptaFallaConMensajeEnEspanol() throws IOException {
        Files.createDirectories(home);
        Files.writeString(home.resolve(ConfigManager.CONFIG_FILE_NAME), "{ roto", StandardCharsets.UTF_8);

        Result result = run("status", "--corpus", corpus.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().startsWith("Error: "));
        assertTrue(result.err().contains("No se pudo leer la configuración"));
    }

    private void writeConfig() {
        new ConfigManager(home).save(AppConfig.builder()
                .ollamaBaseUrl(server.baseUrl())
                .chatModel("qwen3:8b")
                .embeddingModel("embeddinggemma")
                .chunkSizeTokens(400)
                .chunkOverlapTokens(80)
                .embeddingContextTokens(2048)
                .vectorStore(StoreType.JSON)
                .build());
    }

    private Result run(String... args) {
        CommandLine commandLine = Main.createCommandLine();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        redirect(commandLine, new PrintWriter(out, true), new PrintWriter(err, true));
        int exitCode = commandLine.execute(args);
        return new Result(exitCode, out.toString(), err.toString());
    }

    private static void redirect(CommandLine commandLine, PrintWriter out, PrintWriter err) {
        commandLine.setOut(out);
        commandLine.setErr(err);
        for (CommandLine subcommand : commandLine.getSubcommands().values()) {
            redirect(subcommand, out, err);
        }
    }

    private record Result(int exitCode, String out, String err) {
    }
}
