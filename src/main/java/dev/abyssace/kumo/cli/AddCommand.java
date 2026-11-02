package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.EmbeddingContextGuard;
import dev.abyssace.kumo.llm.HttpOllamaClient;
import dev.abyssace.kumo.llm.ModelNames;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaModel;
import dev.abyssace.kumo.rag.IndexingService;
import dev.abyssace.kumo.rag.IndexingSummary;
import dev.abyssace.kumo.render.ConsoleIndexingListener;
import dev.abyssace.kumo.render.ConsoleRenderer;
import dev.abyssace.kumo.store.JsonVectorStore;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Patrón: Command + Observer.
 *
 * <p>Indexa archivos o carpetas. Antes de tocar el índice comprueba que el
 * modelo de embeddings esté instalado y que el fragmento quepa en su contexto.
 * Un archivo ilegible no aborta el lote; un fallo de Ollama o del formato del
 * índice sí, porque afecta a todos.</p>
 */
@Command(name = "add", description = Messages.CLI_CMD_ADD)
public final class AddCommand implements Callable<Integer> {

    @Mixin
    private HelpOption helpOption;

    @Mixin
    private CorpusOption corpusOption;

    @Option(names = {"-v", "--verbose"}, description = Messages.CLI_OPT_VERBOSE)
    private boolean verbose;

    @Parameters(
            arity = "1..*",
            paramLabel = "<ruta>",
            description = Messages.CLI_OPT_PATHS)
    private List<Path> paths = new ArrayList<>();

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        Path corpus = corpusOption.resolve();

        ConfigManager manager = new ConfigManager();
        AppConfig config = manager.load();
        HttpOllamaClient ollama = CliFactory.ollama(config);

        List<String> installed = ollama.listModels().stream().map(OllamaModel::name).toList();
        if (!ModelNames.isInstalled(config.embeddingModel(), installed)) {
            throw new OllamaException(Messages.OLLAMA_MODEL_NOT_INSTALLED.formatted(
                    config.embeddingModel(), config.embeddingModel()));
        }
        EmbeddingContextGuard.validate(config, ollama, manager.configFile());

        List<Path> existing = new ArrayList<>();
        int missing = 0;
        for (Path path : paths) {
            Path normalized = path.toAbsolutePath().normalize();
            if (Files.exists(normalized)) {
                existing.add(normalized);
            } else {
                err.println(Messages.CLI_ERROR.formatted(
                        Messages.FILE_NOT_FOUND.formatted(normalized)));
                missing++;
            }
        }
        if (existing.isEmpty()) {
            err.flush();
            return 1;
        }

        ConsoleIndexingListener listener = new ConsoleIndexingListener(
                out, verbose || config.verbose(), corpus);
        IndexingService indexing = CliFactory.indexing(config, corpus, ollama);
        IndexingSummary summary = indexing.index(existing, listener);

        ConsoleRenderer renderer = new ConsoleRenderer(out);
        renderer.printIndexingSummary(summary, JsonVectorStore.indexPath(corpus));

        if (summary.isEmpty()) {
            err.println(Messages.NO_SUPPORTED_FILES.formatted(
                    String.join(", ", indexing.supportedExtensions()), corpus));
            err.flush();
            return 1;
        }

        out.flush();
        err.flush();
        return summary.hasFailures() || missing > 0 ? 1 : 0;
    }
}
