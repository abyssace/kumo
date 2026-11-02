package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import picocli.CommandLine;

/**
 * Patrón: Chain of Responsibility para errores de argumentos.
 *
 * <p>Picocli genera los mensajes de sintaxis en inglés. Este manejador los
 * traduce a español y muestra el uso del comando afectado, sin imprimir la
 * traza de Java. Los errores de ejecución los trata {@link CliExceptionHandler}.</p>
 */
public final class CliParameterExceptionHandler implements CommandLine.IParameterExceptionHandler {

    @Override
    public int handleParseException(CommandLine.ParameterException ex, String[] args) {
        CommandLine commandLine = ex.getCommandLine();
        PrintWriter err = commandLine.getErr();
        err.println(Messages.CLI_ERROR.formatted(describe(ex)));
        commandLine.usage(err);
        err.flush();
        return commandLine.getCommandSpec().exitCodeOnInvalidInput();
    }

    private static String describe(CommandLine.ParameterException ex) {
        if (ex instanceof CommandLine.UnmatchedArgumentException unmatched) {
            return Messages.CLI_UNKNOWN_ARGUMENT.formatted(String.join(" ", unmatched.getUnmatched()));
        }
        if (ex instanceof CommandLine.MissingParameterException missing) {
            String names = missing.getMissing().stream()
                    .map(CommandLine.Model.ArgSpec::paramLabel)
                    .collect(Collectors.joining(", "));
            return Messages.CLI_MISSING_PARAMETER.formatted(names);
        }
        if (ex.getCause() instanceof CommandLine.TypeConversionException conversion) {
            return Messages.CLI_INVALID_VALUE.formatted(conversion.getMessage());
        }
        return Messages.CLI_INVALID_ARGUMENTS.formatted(ex.getMessage());
    }
}
