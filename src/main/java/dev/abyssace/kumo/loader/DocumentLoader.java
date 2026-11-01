package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.Document;
import java.nio.file.Path;

/**
 * Patrón: Strategy (contrato) + Template Method (implementaciones concretas).
 *
 * <p>Cada formato de archivo aporta su propio loader. El resto del sistema
 * depende solo de esta interfaz, no de PDFBox ni de java.nio.</p>
 */
public interface DocumentLoader {

    /** Indica si este loader puede leer el archivo indicado. */
    boolean supports(Path path);

    /** Carga el archivo completo. */
    Document load(Path path);
}
