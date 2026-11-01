package dev.abyssace.kumo.domain;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Fragmento junto a su vector de embedding. Como los arreglos son mutables,
 * el constructor y el accessor copian el vector para que el record se comporte
 * realmente como inmutable.</p>
 *
 * @param chunk  fragmento original
 * @param vector vector de embedding, no vacío
 */
public record EmbeddedChunk(Chunk chunk, float[] vector) {

    public EmbeddedChunk {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(vector, "vector");
        if (vector.length == 0) {
            throw new IllegalArgumentException("El vector de embedding no puede estar vacío");
        }
        vector = vector.clone();
    }

    /** Devuelve una copia defensiva del vector. */
    @Override
    public float[] vector() {
        return vector.clone();
    }
}
