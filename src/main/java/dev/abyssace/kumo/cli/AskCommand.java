package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.HttpOllamaClient;
import dev.abyssace.kumo.rag.RagService;
import dev.abyssace.kumo.render.ConsoleRenderer;
import dev.abyssace.kumo.store.VectorStore;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Patrón: Command + Observer de tokens.
 *
 * <p>Recupera contexto del índice y transmite la respuesta del modelo según se
 * genera. Al terminar imprime las fuentes; {@code --verbose} añade la
 * similitud de cada fragmento.</p>
 */
@Command(name = "ask", description = Messages.CLI_CMD_ASK)
public final class AskCommand implements Callable<Integer> {

    @Mixin
    private HelpOption helpOption;

    @Mixin
    private CorpusOption corpusOption;

    @Option(names = {"-v", "--verbose"}, description = Messages.CLI_OPT_VERBOSE)
    private boolean verbose;

    @Option(names = {"-k", "--top-k"}, paramLabel = "<n>", description = Messages.CLI_OPT_TOP_K)
    private Integer topK;

    @Parameters(
            index = "0",
            arity = "1",
            paramLabel = "<pregunta>",
            description = Messages.CLI_OPT_QUESTION)
    private String question;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        Path corpus = corpusOption.resolve();

        if (question == null || question.isBlank()) {
            err.println(Messages.QUESTION_EMPTY);
            err.flush();
            return 1;
        }
        if (topK != null && topK < 1) {
            throw new IllegalArgumentException(Messages.TOP_K_INVALID.formatted(topK));
        }

        ConfigManager manager = new ConfigManager();
        AppConfig config = manager.load();
        int effectiveTopK = topK == null ? config.topK() : topK;

        HttpOllamaClient ollama = CliFactory.ollama(config);
        VectorStore store = CliFactory.store(config, corpus);
        if (store.size() == 0) {
            err.println(Messages.INDEX_EMPTY);
            err.flush();
            return 1;
        }

        RagService rag = CliFactory.rag(config, store, ollama, effectiveTopK);
        ConsoleRenderer renderer = new ConsoleRenderer(out);

        Answer answer = rag.askStreaming(question, token -> {
            out.print(token);
            out.flush();
        });
        out.println();
        if (answer.text().isBlank()) {
            out.println(Messages.EMPTY_ANSWER);
        }
        renderer.printCitations(answer.citations(), verbose || config.verbose());
        out.flush();
        return 0;
    }
}
