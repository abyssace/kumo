package dev.abyssace.kumo.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Data Transfer Object (record) + Mapper.
 *
 * <p>Representación serializable del índice. Es deliberadamente plana y
 * legible: un JSON con metadatos arriba ({@code formatVersion},
 * {@code embeddingModel}, {@code dimensions}) y la lista de fragmentos debajo.
 * El dominio no conoce a Jackson; la conversión ocurre aquí.</p>
 *
 * <p>{@code formatVersion} permite evolucionar el archivo sin adivinar: si una
 * versión futura cambia el esquema, la actual puede rechazarlo con un mensaje
 * claro en lugar de fallar de forma extraña.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IndexFile(
        int formatVersion,
        String embeddingModel,
        int dimensions,
        List<StoredChunk> chunks) {

    /** Versión de esquema que escribe esta versión de kumo. */
    public static final int CURRENT_FORMAT_VERSION = 1;

    public IndexFile {
        Objects.requireNonNull(embeddingModel, "embeddingModel");
        Objects.requireNonNull(chunks, "chunks");
        chunks = List.copyOf(chunks);
    }

    /** Un fragmento tal como se guarda en disco. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StoredChunk(
            String id,
            String source,
            int index,
            String text,
            StoredLocation location,
            float[] vector) {

        public StoredChunk {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(vector, "vector");
            vector = vector.clone();
        }

        /** Devuelve una copia defensiva del vector. */
        @Override
        public float[] vector() {
            return vector.clone();
        }

        public static StoredChunk from(EmbeddedChunk embedded) {
            Objects.requireNonNull(embedded, "embedded");
            Chunk chunk = embedded.chunk();
            return new StoredChunk(
                    chunk.id(),
                    chunk.source().toString(),
                    chunk.index(),
                    chunk.text(),
                    StoredLocation.from(chunk.location()),
                    embedded.vector());
        }

        public EmbeddedChunk toEmbedded() {
            Chunk chunk = new Chunk(id, Path.of(source), index, text, location.toSourceLocation());
            return new EmbeddedChunk(chunk, vector);
        }
    }

    /**
     * Ubicación serializable. Se usa un objeto con {@code type} explícito en
     * lugar de una unión implícita: el JSON se lee solo y admite ubicaciones
     * nuevas sin romper a los lectores antiguos.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StoredLocation(String type, Integer page, Integer startLine, Integer endLine) {

        public static final String TYPE_PAGE = "page";
        public static final String TYPE_LINES = "lines";
        public static final String TYPE_NONE = "none";

        public static StoredLocation from(SourceLocation location) {
            Objects.requireNonNull(location, "location");
            return switch (location) {
                case SourceLocation.PageRef pageRef ->
                        new StoredLocation(TYPE_PAGE, pageRef.page(), null, null);
                case SourceLocation.LineRange range ->
                        new StoredLocation(TYPE_LINES, null, range.startLine(), range.endLine());
                case SourceLocation.None ignored ->
                        new StoredLocation(TYPE_NONE, null, null, null);
            };
        }

        public SourceLocation toSourceLocation() {
            return switch (type) {
                case TYPE_PAGE -> new SourceLocation.PageRef(require(page, "page"));
                case TYPE_LINES -> new SourceLocation.LineRange(
                        require(startLine, "startLine"), require(endLine, "endLine"));
                case TYPE_NONE -> SourceLocation.none();
                default -> throw new IndexFormatException("Ubicación desconocida en el índice: " + type);
            };
        }

        private static int require(Integer value, String field) {
            if (value == null) {
                throw new IndexFormatException("Falta el campo '" + field + "' en la ubicación del índice");
            }
            return value;
        }
    }
}
