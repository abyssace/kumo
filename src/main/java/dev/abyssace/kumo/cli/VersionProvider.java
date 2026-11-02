package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import picocli.CommandLine;

/**
 * Patrón: Provider.
 *
 * <p>Obtiene la versión del manifiesto del jar. Al ejecutar desde clases
 * compiladas (tests, {@code mvn exec:java}) no hay manifiesto y se muestra
 * {@code dev} en lugar de una versión inventada.</p>
 */
public final class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = Messages.CLI_DEV_VERSION;
        }
        return new String[] {Messages.CLI_VERSION.formatted(version)};
    }
}
