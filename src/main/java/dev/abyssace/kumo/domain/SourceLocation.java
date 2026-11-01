package dev.abyssace.kumo.domain;

/**
 * Patrón: Sum Type (sealed interface) sobre un Value Object.
 *
 * <p>Ubicación de origen de un fragmento. Permite citar con la precisión que
 * cada formato admite: página en PDF, rango de líneas en TXT/MD o nada.</p>
 */
public sealed interface SourceLocation
        permits SourceLocation.PageRef, SourceLocation.LineRange, SourceLocation.None {

    /**
     * Texto breve para humanos, en español, p. ej. {@code "página 3"}.
     * Devuelve cadena vacía cuando no hay ubicación.
     */
    String describe();

    /** Ubicación por número de página (PDF). */
    record PageRef(int page) implements SourceLocation {

        public PageRef {
            if (page < 1) {
                throw new IllegalArgumentException("La página debe ser >= 1, pero fue " + page);
            }
        }

        @Override
        public String describe() {
            return "página " + page;
        }
    }

    /** Ubicación por rango de líneas, ambas inclusive (TXT/MD). */
    record LineRange(int startLine, int endLine) implements SourceLocation {

        public LineRange {
            if (startLine < 1) {
                throw new IllegalArgumentException("La línea inicial debe ser >= 1, pero fue " + startLine);
            }
            if (endLine < startLine) {
                throw new IllegalArgumentException(
                        "La línea final (" + endLine + ") no puede ser menor que la inicial (" + startLine + ")");
            }
        }

        @Override
        public String describe() {
            return startLine == endLine
                    ? "línea " + startLine
                    : "líneas " + startLine + "-" + endLine;
        }
    }

    /** Sin ubicación conocida. */
    record None() implements SourceLocation {

        @Override
        public String describe() {
            return "";
        }
    }

    /** Atajo para el caso sin ubicación. */
    static SourceLocation none() {
        return new None();
    }
}
