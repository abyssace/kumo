package dev.abyssace.kumo.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.config.BriefTemplateManager;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.loader.DocumentDiscovery;
import dev.abyssace.kumo.loader.DocumentLoaderFactory;
import dev.abyssace.kumo.loader.SourceCodeDocumentLoader;
import dev.abyssace.kumo.render.ConsoleSummaryListener;
import dev.abyssace.kumo.render.TaskOutputWriter;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Patrón: Command. Procesa archivos uno a uno y publica solo un JSON completo y válido. */
@Command(name = "brief", description = Messages.CLI_CMD_BRIEF)
public final class BriefCommand implements Callable<Integer> {
    @Mixin private HelpOption helpOption;
    @Parameters(index = "0", arity = "1", paramLabel = "<ruta>", description = Messages.BRIEF_PATH)
    private Path input;
    @Option(names = "--template", paramLabel = "<plantilla.json>", description = Messages.BRIEF_TEMPLATE_OPTION)
    private Path template;
    @Option(names = "--language", paramLabel = "<idioma>", description = Messages.SUMMARY_LANGUAGE_OPTION)
    private String language;
    @Option(names = {"-o", "--output"}, paramLabel = "<ficha.json>", description = Messages.TASK_OUTPUT_OPTION)
    private Path output;
    @Spec private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        var err = spec.commandLine().getErr();
        var out = spec.commandLine().getOut();
        SummaryLanguage selected = language == null ? SummaryLanguage.DOCUMENT : SummaryLanguage.parse(language);
        TaskOutputWriter writer = new TaskOutputWriter();
        Path target = output == null ? null : output.toAbsolutePath().normalize();
        if (target != null) {
            writer.validate(target);
        }
        var loaders = DocumentLoaderFactory.forSummaries();
        var files = new DocumentDiscovery().discover(input, loaders);
        var manager = new ConfigManager();
        var templates = new BriefTemplateManager(manager.home());
        var settings = templates.load(template);
        err.println(Messages.BRIEF_TEMPLATE_PATH.formatted(template == null ? templates.defaultPath() : template));
        var config = manager.load();
        var service = CliFactory.brief(config, CliFactory.ollama(config));
        var mapper = new ObjectMapper();
        var results = mapper.createArrayNode();
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            err.println(Messages.BRIEF_FILE.formatted(i + 1, files.size(), file));
            err.flush();
            results.add(service.extract(loaders.load(file), settings, selected,
                    new SourceCodeDocumentLoader().supports(file), new ConsoleSummaryListener(err)));
        }
        var result = Files.isDirectory(input) ? mapper.createObjectNode().set("results", results) : results.get(0);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        if (target != null) {
            writer.write(target, json);
        }
        out.println(json);
        out.flush();
        return 0;
    }
}
