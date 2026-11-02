package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.render.ConsoleSummaryListener;
import dev.abyssace.kumo.render.TaskOutputWriter;
import dev.abyssace.kumo.render.WriteMarkdownRenderer;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Patrón: Command. Redacción completa con progreso y recuperación más amplia que ask. */
@Command(name = "write", description = Messages.CLI_CMD_WRITE)
public final class WriteCommand implements Callable<Integer> {
    @Mixin private HelpOption helpOption;
    @Mixin private CorpusOption corpusOption;
    @Parameters(index = "0", arity = "1", paramLabel = "<instrucción>", description = Messages.WRITE_INSTRUCTION)
    private String instruction;
    @Option(names = "--language", paramLabel = "<idioma>", description = Messages.WRITE_LANGUAGE_OPTION)
    private String language;
    @Option(names = {"-k", "--top-k"}, paramLabel = "<n>", description = Messages.WRITE_TOP_K_OPTION)
    private Integer topK;
    @Option(names = {"-v", "--verbose"}, description = Messages.CLI_OPT_VERBOSE)
    private boolean verbose;
    @Option(names = {"-o", "--output"}, paramLabel = "<documento.md>", description = Messages.TASK_OUTPUT_OPTION)
    private Path output;
    @Spec private CommandSpec spec;

    @Override
    public Integer call() {
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        SummaryLanguage selected = language == null ? SummaryLanguage.DOCUMENT : SummaryLanguage.parse(language);
        if (instruction.isBlank()) {
            throw new IllegalArgumentException(Messages.WRITE_INSTRUCTION);
        }
        TaskOutputWriter writer = new TaskOutputWriter();
        Path target = output == null ? null : output.toAbsolutePath().normalize();
        if (target != null) {
            writer.validate(target);
        }
        var config = new ConfigManager().load();
        int effectiveTopK = topK == null ? config.writeTopK() : topK;
        if (effectiveTopK < 1) {
            throw new IllegalArgumentException(Messages.WRITE_TOP_K_INVALID);
        }
        var store = CliFactory.store(config, corpusOption.resolve());
        err.println(Messages.WRITE_COVERAGE.formatted(effectiveTopK));
        err.flush();
        var answer = CliFactory.write(config, store, CliFactory.ollama(config))
                .write(instruction, selected, effectiveTopK, new ConsoleSummaryListener(err));
        String markdown = new WriteMarkdownRenderer().render(answer, selected,
                verbose || config.verbose(), effectiveTopK);
        if (target != null) {
            writer.write(target, markdown);
        }
        out.println(markdown);
        out.flush();
        return 0;
    }
}
