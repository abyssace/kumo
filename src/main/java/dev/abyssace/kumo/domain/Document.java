package dev.abyssace.kumo.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Documento cargado desde disco. Siempre conserva la ruta absoluta y
 * normalizada para que las citas sean inequívocas. El texto se organiza en
 * páginas: un TXT/MD tiene una sola página, un PDF una por página real.</p>
 *
 * @param path   ruta absoluta del archivo de origen
 * @param title  título legible; si viene vacío se usa el nombre del archivo
 * @param format formato de origen
 * @param pages  páginas no vacías, en orden
 */
public record Document(Path path, String title, DocumentFormat format, List<DocumentPage> pages) {

    public Document {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(pages, "pages");

        path = path.toAbsolutePath().normalize();
        title = (title == null || title.isBlank()) ? fileName(path) : title.strip();

        if (pages.isEmpty()) {
            throw new IllegalArgumentException("El documento no tiene páginas: " + path);
        }
        pages = List.copyOf(pages);
    }

    /**
     * Texto completo del documento, con las páginas separadas por un salto de
     * línea. Se calcula bajo demanda para no duplicar memoria.
     */
    public String text() {
        return pages.stream()
                .map(DocumentPage::text)
                .collect(Collectors.joining("\n"));
    }

    private static String fileName(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }
}
