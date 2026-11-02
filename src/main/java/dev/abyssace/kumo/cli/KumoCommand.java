package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Patrón: Command (raíz) + Composite.
 *
 * <p>Agrupa los subcomandos y centraliza la ayuda y la versión. Sin argumentos
 * muestra el uso en lugar de fallar, para que un primer contacto con la
 * herramienta sea informativo.</p>
 */
@Command(
        name = "kumo",
        versionProvider = VersionProvider.class,
        description = Messages.CLI_DESCRIPTION,
        subcommands = {
            InitCommand.class,
            AddCommand.class,
            AskCommand.class,
            StatusCommand.class
        })
public final class KumoCommand implements Runnable {

    @Mixin
    private HelpOption helpOption;

    @Mixin
    private VersionOption versionOption;

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
