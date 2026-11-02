package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import picocli.CommandLine.Option;

/**
 * Patrón: Mixin de opciones.
 *
 * <p>La versión solo tiene sentido en el comando raíz; los subcomandos usan
 * únicamente {@link HelpOption}.</p>
 */
public final class VersionOption {

    @Option(names = {"-V", "--version"}, versionHelp = true, description = Messages.CLI_OPT_VERSION)
    private boolean version;
}
