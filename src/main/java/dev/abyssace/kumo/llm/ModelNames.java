package dev.abyssace.kumo.llm;

import java.util.List;
import java.util.Objects;

/**
 * Patrón: Utility / Specification.
 *
 * <p>Ollama normaliza los nombres sin tag a {@code :latest}. Esta clase evita
 * que {@code status} diga que falta un modelo solo porque la configuración
 * escriba {@code embeddinggemma} y Ollama lo devuelva como
 * {@code embeddinggemma:latest}.</p>
 */
public final class ModelNames {

    private ModelNames() {
    }

    /** Indica si {@code configured} está entre los modelos instalados. */
    public static boolean isInstalled(String configured, List<String> installed) {
        Objects.requireNonNull(configured, "configured");
        Objects.requireNonNull(installed, "installed");
        String target = configured.strip();
        for (String name : installed) {
            if (name.equals(target)) {
                return true;
            }
            if (!target.contains(":") && name.equals(target + ":latest")) {
                return true;
            }
        }
        return false;
    }
}
