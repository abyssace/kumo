package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.config.ConfigException;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.ModelContextException;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.loader.DocumentLoadException;
import dev.abyssace.kumo.store.IndexFormatException;
import dev.abyssace.kumo.store.IndexPersistenceException;
import dev.abyssace.kumo.summarize.SummaryException;
import picocli.CommandLine;

/**
 * Patrón: Chain of Responsibility para errores de ejecución.
 *
 * <p>Traduce las excepciones de dominio a un mensaje en español y un código de
 * salida 1, sin mostrar trazas de Java. Las excepciones desconocidas se
 * resumen con su tipo para no filtrar detalles internos al usuario.</p>
 */
public final class CliExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(
            Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        commandLine.getErr().println(userMessage(ex));
        commandLine.getErr().flush();
        return 1;
    }

    private static String userMessage(Exception ex) {
        if (isDomainException(ex)) {
            String message = ex.getMessage();
            if (message != null && !message.isBlank()) {
                return Messages.CLI_ERROR.formatted(message);
            }
        }
        return Messages.CLI_UNEXPECTED_ERROR.formatted(ex.getClass().getSimpleName());
    }

    private static boolean isDomainException(Exception ex) {
        return ex instanceof SummaryException
                || ex instanceof ConfigException
                || ex instanceof OllamaException
                || ex instanceof IndexFormatException
                || ex instanceof IndexPersistenceException
                || ex instanceof DocumentLoadException
                || ex instanceof ModelContextException
                || ex instanceof IllegalArgumentException;
    }
}
