package dev.abyssace.kumo.domain;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Una página lógica del documento. Los formatos de texto plano usan una sola
 * página; los PDF conservan la numeración real para poder citar por página.</p>
 *
 * @param number número de página, empezando en 1
 * @param text   texto extraído de esa página
 */
public record DocumentPage(int number, String text) {

    public DocumentPage {
        if (number < 1) {
            throw new IllegalArgumentException("El número de página debe ser >= 1, pero fue " + number);
        }
        Objects.requireNonNull(text, "text");
    }
}
