package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Patrón: Template Method.
 *
 * <p>Fija el esqueleto de la carga (validar ruta, leer páginas, construir el
 * {@link Document}) y deja a cada formato solo dos decisiones: qué extensiones
 * acepta y cómo extrae sus páginas.</p>
 */
public abstract class AbstractDocumentLoader implements DocumentLoader {

    @Override
    public final boolean supports(Path path) {
        Objects.requireNonNull(path, "path");
        String fileName = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extensions().contains(extension);
    }

    @Override
    public final Document load(Path path) {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();

        if (!supports(normalized)) {
            throw new DocumentLoadException(
                    getClass().getSimpleName() + " no admite el archivo: " + normalized);
        }
        if (!Files.isRegularFile(normalized)) {
            throw new DocumentLoadException("No existe el archivo o no es un archivo regular: " + normalized);
        }

        try {
            List<DocumentPage> pages = readPages(normalized);
            if (pages.isEmpty()) {
                throw new DocumentLoadException("El documento no contiene texto extraíble: " + normalized);
            }
            return new Document(normalized, title(normalized), format(), pages);
        } catch (IOException ex) {
            throw new DocumentLoadException("No se pudo leer " + normalized + ": " + ex.getMessage(), ex);
        }
    }

    /** Extensiones aceptadas, en minúsculas y sin punto. */
    protected abstract Set<String> extensions();

    /** Formato que produce este loader. */
    protected abstract DocumentFormat format();

    /** Extrae las páginas. Cada loader decide cómo. */
    protected abstract List<DocumentPage> readPages(Path path) throws IOException;

    /** Título por defecto: el nombre del archivo. */
    protected String title(Path path) {
        return path.getFileName().toString();
    }
}
