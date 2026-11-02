package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.EmbeddingContextGuard;
import dev.abyssace.kumo.llm.HttpOllamaClient;
import dev.abyssace.kumo.llm.ModelContextException;
import dev.abyssace.kumo.llm.ModelNames;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaModel;
import dev.abyssace.kumo.llm.OllamaModelInfo;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Patrón: Command.
 *
 * <p>Crea la configuración global si no existe y comprueba, sin descargar
 * nada, si Ollama y los modelos configurados están disponibles. Si Ollama no
 * responde, avisa y sale con 0: instalar o arrancar servicios es decisión del
 * usuario, no un requisito para terminar la configuración.</p>
 */
@Command(name = "init", description = Messages.CLI_CMD_INIT)
public final class InitCommand implements Callable<Integer> {

    @Mixin
    private HelpOption helpOption;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        ConfigManager manager = new ConfigManager();
        boolean existed = manager.exists();
        AppConfig config = existed ? manager.load() : manager.loadOrCreate();

        out.println(existed
                ? Messages.CLI_INIT_FOUND.formatted(manager.configFile())
                : Messages.CLI_INIT_CREATED.formatted(manager.configFile()));

        HttpOllamaClient ollama = CliFactory.ollama(config);
        List<OllamaModel> models;
        try {
            models = ollama.listModels();
        } catch (OllamaException ex) {
            out.println(ex.getMessage());
            out.println(Messages.CLI_INIT_MODELS_HINT);
            out.println(Messages.CLI_INIT_HINT);
            out.flush();
            return 0;
        }

        out.println(Messages.OLLAMA_CONNECTED.formatted(config.ollamaBaseUrl()));
        List<String> names = models.stream().map(OllamaModel::name).toList();
        boolean chatInstalled = ModelNames.isInstalled(config.chatModel(), names);
        boolean embeddingInstalled = ModelNames.isInstalled(config.embeddingModel(), names);

        printModel(out, config.chatModel(), chatInstalled);
        printModel(out, config.embeddingModel(), embeddingInstalled);
        if (!chatInstalled) {
            out.println(Messages.OLLAMA_MODEL_HINT.formatted(config.chatModel()));
        }
        if (!embeddingInstalled) {
            out.println(Messages.OLLAMA_MODEL_HINT.formatted(config.embeddingModel()));
        }

        if (embeddingInstalled) {
            printContext(out, config, ollama, manager);
        }

        out.println(Messages.CLI_INIT_HINT);
        out.flush();
        return 0;
    }

    private static void printModel(PrintWriter out, String model, boolean installed) {
        out.println(Messages.OLLAMA_MODEL_LINE.formatted(
                model,
                installed ? Messages.OLLAMA_MODEL_INSTALLED : Messages.OLLAMA_MODEL_MISSING));
    }

    private static void printContext(
            PrintWriter out, AppConfig config, HttpOllamaClient ollama, ConfigManager manager) {
        try {
            OllamaModelInfo info = EmbeddingContextGuard.validate(
                    config, ollama, manager.configFile());
            info.contextLength().ifPresent(limit -> out.println(
                    Messages.OLLAMA_MODEL_CONTEXT.formatted(config.embeddingModel(), limit)));
        } catch (ModelContextException ex) {
            out.println(ex.getMessage());
        } catch (OllamaException ignored) {
            // /api/show no respondió; init no debe fallar por un detalle informativo.
        }
    }
}
