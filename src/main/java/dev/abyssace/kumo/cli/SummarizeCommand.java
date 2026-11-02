package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.loader.DocumentLoaderFactory;
import dev.abyssace.kumo.loader.SourceCodeDocumentLoader;
import dev.abyssace.kumo.render.ConsoleSummaryListener;
import dev.abyssace.kumo.render.MarkdownSummaryWriter;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import dev.abyssace.kumo.summarize.SummaryStrategy;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Patrón: Command. Orquesta un archivo, sin crear ni consultar ningún índice. */
@Command(name = "summarize", description = Messages.CLI_CMD_SUMMARIZE)
public final class SummarizeCommand implements Callable<Integer> {
    @Mixin
    private HelpOption helpOption;

    @Parameters(index = "0", arity = "1", paramLabel = "<archivo>", description = Messages.SUMMARY_FILE)
    private Path file;

    @Option(names = "--strategy", defaultValue = "auto", description = Messages.SUMMARY_STRATEGY_OPTION)
    private String strategy;

    @Option(names = "--language", description = Messages.SUMMARY_LANGUAGE_OPTION)
    private String language;

    @Option(names = {"-o", "--output"}, paramLabel = "<resumen.md>", description = Messages.SUMMARY_OUTPUT_OPTION)
    private Path output;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        var err = spec.commandLine().getErr();
        var out = spec.commandLine().getOut();
        SummaryStrategy selected = SummaryStrategy.parse(strategy);
        SummaryLanguage selectedLanguage = language == null
                ? SummaryLanguage.DOCUMENT : SummaryLanguage.parse(language);
        MarkdownSummaryWriter writer = new MarkdownSummaryWriter();
        Path target = output == null ? null : output.toAbsolutePath().normalize();
        if (target != null) {
            writer.validate(target);
        }
        err.println(Messages.SUMMARY_READ.formatted(file.toAbsolutePath().normalize()));
        err.flush();
        var document = DocumentLoaderFactory.forSummaries().load(file);
        boolean code = new SourceCodeDocumentLoader().supports(file);
        if (code) {
            err.println(Messages.SUMMARY_CODE_WARNING);
            err.flush();
        }
        var config = new ConfigManager().load();
        var service = CliFactory.summary(config, CliFactory.ollama(config));
        String markdown = service.summarize(document, selected, selectedLanguage, code,
                new ConsoleSummaryListener(err));
        if (target != null) {
            writer.write(target, markdown);
        }
        out.println(markdown);
        out.flush();
        err.println(Messages.SUMMARY_DONE);
        err.flush();
        return 0;
    }
}
