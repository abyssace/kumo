package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.rag.StatusReport;
import dev.abyssace.kumo.rag.StatusService;
import dev.abyssace.kumo.render.ConsoleRenderer;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Patrón: Command + Facade de diagnóstico.
 *
 * <p>Muestra configuración, estado de Ollama e índice. Está pensado para
 * funcionar cuando algo va mal: si Ollama está caído o el índice es de otro
 * modelo, lo cuenta en lugar de fallar.</p>
 */
@Command(name = "status", description = Messages.CLI_CMD_STATUS)
public final class StatusCommand implements Callable<Integer> {

    @Mixin
    private HelpOption helpOption;

    @Mixin
    private CorpusOption corpusOption;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        Path corpus = corpusOption.path();

        ConfigManager manager = new ConfigManager();
        AppConfig config = manager.load();
        StatusService service = new StatusService(manager, CliFactory.ollama(config));
        StatusReport report = service.report(corpus);

        new ConsoleRenderer(out).printStatus(report);
        out.flush();
        return 0;
    }
}
