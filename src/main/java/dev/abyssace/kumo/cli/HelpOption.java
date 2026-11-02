package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import picocli.CommandLine.Option;

/**
 * Patrón: Mixin de opciones.
 *
 * <p>Sustituye a {@code mixinStandardHelpOptions} de Picocli, que imprime
 * "Usage/Options/Commands" en inglés. Aquí la opción de ayuda es la misma en
 * todos los comandos y su texto vive en {@link Messages}.</p>
 */
public final class HelpOption {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = Messages.CLI_OPT_HELP)
    private boolean help;
}
