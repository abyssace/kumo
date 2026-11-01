package dev.abyssace.kumo.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Fragmento de un documento listo para embedding. El {@code id} es estable
 * ({@code ruta#índice}) para que reindexar reemplace el fragmento en lugar de
 * duplicarlo.</p>
 *
 * @param id       identificador estable dentro del índice
 * @param source   archivo de origen (absoluto y normalizado)
 * @param index    posición del fragmento dentro del documento, desde 0
 * @param text     contenido del fragmento
 * @param location ubicación de origen para las citas
 */
public record Chunk(String id, Path source, int index, String text, SourceLocation location) {

    public Chunk {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(location, "location");

        if (index < 0) {
            throw new IllegalArgumentException("El índice del fragmento debe ser >= 0, pero fue " + index);
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("El texto del fragmento no puede estar vacío: " + id);
        }

        source = source.toAbsolutePath().normalize();
    }

    /**
     * Crea un fragmento calculando su identificador estable a partir de la ruta
     * y el índice.
     */
    public static Chunk of(Path source, int index, String text, SourceLocation location) {
        Objects.requireNonNull(source, "source");
        Path normalized = source.toAbsolutePath().normalize();
        return new Chunk(normalized + "#" + index, normalized, index, text, location);
    }

    /** Referencia corta para consola, p. ej. {@code informe.pdf#3}. */
    public String reference() {
        Path name = source.getFileName();
        return (name == null ? source.toString() : name.toString()) + "#" + index;
    }
}
