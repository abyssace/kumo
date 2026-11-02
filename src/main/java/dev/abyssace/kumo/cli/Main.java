package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import picocli.CommandLine;
import picocli.CommandLine.Model.UsageMessageSpec;

/**
 * Patrón: Entry Point + Factory Method.
 *
 * <p>Punto de entrada del jar. {@link #createCommandLine()} se expone para que
 * las pruebas ejecuten la CLI sin lanzar {@link System#exit(int)}. También
 * aplica los textos en español a la ayuda y a los errores de sintaxis de
 * Picocli, que por defecto vienen en inglés.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(createCommandLine().execute(args));
    }

    /** Construye la CLI con ayuda, errores y textos comunes. */
    public static CommandLine createCommandLine() {
        CommandLine commandLine = new CommandLine(new KumoCommand());
        configure(commandLine);
        return commandLine;
    }

    private static void configure(CommandLine commandLine) {
        CommandLine.IExecutionExceptionHandler executionHandler = new CliExceptionHandler();
        CommandLine.IParameterExceptionHandler parameterHandler = new CliParameterExceptionHandler();
        configureTree(commandLine, executionHandler, parameterHandler);
    }

    private static void configureTree(
            CommandLine commandLine,
            CommandLine.IExecutionExceptionHandler executionHandler,
            CommandLine.IParameterExceptionHandler parameterHandler) {
        commandLine.setExecutionExceptionHandler(executionHandler);
        commandLine.setParameterExceptionHandler(parameterHandler);

        UsageMessageSpec usage = commandLine.getCommandSpec().usageMessage();
        usage.synopsisHeading(Messages.CLI_USAGE_HEADING);
        usage.optionListHeading(Messages.CLI_OPTIONS_HEADING);
        usage.parameterListHeading(Messages.CLI_PARAMETERS_HEADING);
        usage.commandListHeading(Messages.CLI_COMMANDS_HEADING);
        usage.descriptionHeading(Messages.CLI_DESCRIPTION_HEADING);

        for (CommandLine subcommand : commandLine.getSubcommands().values()) {
            configureTree(subcommand, executionHandler, parameterHandler);
        }
    }
}
