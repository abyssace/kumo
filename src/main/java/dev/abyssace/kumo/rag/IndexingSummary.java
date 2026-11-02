package dev.abyssace.kumo.rag;

/**
 * Patrón: Value Object (record inmutable).
 *
 * @param files    archivos indexados correctamente
 * @param chunks   fragmentos escritos en el índice
 * @param failures archivos que no se pudieron indexar
 */
public record IndexingSummary(int files, int chunks, int failures) {

    public IndexingSummary {
        if (files < 0) {
            throw new IllegalArgumentException("files debe ser >= 0, pero fue " + files);
        }
        if (chunks < 0) {
            throw new IllegalArgumentException("chunks debe ser >= 0, pero fue " + chunks);
        }
        if (failures < 0) {
            throw new IllegalArgumentException("failures debe ser >= 0, pero fue " + failures);
        }
    }

    public boolean hasFailures() {
        return failures > 0;
    }

    public boolean isEmpty() {
        return files == 0 && chunks == 0 && failures == 0;
    }
}
