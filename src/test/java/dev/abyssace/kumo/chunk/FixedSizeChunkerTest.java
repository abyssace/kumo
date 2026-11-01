package dev.abyssace.kumo.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.domain.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FixedSizeChunkerTest {

    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();

    @Test
    void divideEnFragmentosQueCabenEnElLimite() {
        String text = String.join(" ", java.util.Collections.nCopies(200, "palabra"));
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, text)));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 40, 0);

        List<Chunk> chunks = chunker.split(document);

        assertTrue(chunks.size() > 1);
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            assertFalse(chunk.text().isBlank());
            assertTrue(estimator.estimate(chunk.text()) <= 40, "Fragmento demasiado grande: " + chunk.text());
            assertEquals(i, chunk.index());
            assertEquals(document.path(), chunk.source());
        }
    }

    @Test
    void losFragmentosConsecutivosSeSolapan() {
        String text = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "palabra" + i)
                .collect(java.util.stream.Collectors.joining(" "));
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, text)));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 40, 10);

        List<Chunk> chunks = chunker.split(document);

        assertTrue(chunks.size() > 1);
        String[] first = chunks.get(0).text().split("\\s+");
        String[] second = chunks.get(1).text().split("\\s+");

        int overlap = 0;
        for (int k = Math.min(first.length, second.length); k >= 1; k--) {
            boolean matches = true;
            for (int j = 0; j < k; j++) {
                if (!first[first.length - k + j].equals(second[j])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                overlap = k;
                break;
            }
        }
        assertTrue(overlap > 0, "Los fragmentos consecutivos deberían compartir palabras");
    }

    @Test
    void calculaRangosDeLineasEnTextoPlano() {
        String text = "línea uno\nlínea dos\nlínea tres\nlínea cuatro";
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, text)));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 5, 0);

        List<Chunk> chunks = chunker.split(document);

        assertTrue(chunks.size() > 1);
        SourceLocation.LineRange first = assertInstanceOf(
                SourceLocation.LineRange.class, chunks.get(0).location());
        assertEquals(1, first.startLine());
        for (Chunk chunk : chunks) {
            SourceLocation.LineRange range = assertInstanceOf(
                    SourceLocation.LineRange.class, chunk.location());
            assertTrue(range.startLine() >= 1);
            assertTrue(range.endLine() <= 4);
        }
    }

    @Test
    void usaLaPaginaComoUbicacionEnPdf() {
        Document document = document(
                DocumentFormat.PDF,
                List.of(
                        new DocumentPage(2, "contenido de la pagina dos"),
                        new DocumentPage(5, "contenido de la pagina cinco")));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 100, 0);

        List<Chunk> chunks = chunker.split(document);

        assertEquals(2, chunks.size());
        assertEquals(new SourceLocation.PageRef(2), chunks.get(0).location());
        assertEquals(new SourceLocation.PageRef(5), chunks.get(1).location());
        assertEquals(0, chunks.get(0).index());
        assertEquals(1, chunks.get(1).index());
    }

    @Test
    void mantieneElIndiceGlobalEntrePaginasDeUnPdf() {
        Document document = document(
                DocumentFormat.PDF,
                List.of(
                        new DocumentPage(1, String.join(" ", java.util.Collections.nCopies(60, "alfa"))),
                        new DocumentPage(2, String.join(" ", java.util.Collections.nCopies(60, "beta")))));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 20, 0);

        List<Chunk> chunks = chunker.split(document);

        assertTrue(chunks.size() > 2);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
        }
        assertEquals(new SourceLocation.PageRef(1), chunks.get(0).location());
        assertEquals(
                new SourceLocation.PageRef(2),
                chunks.get(chunks.size() - 1).location());
    }

    @Test
    void unaPalabraGiganteProduceUnSoloFragmento() {
        String gigante = "x".repeat(1000);
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, gigante)));

        List<Chunk> chunks = new FixedSizeChunker(estimator, 10, 2).split(document);

        assertEquals(1, chunks.size());
        assertEquals(gigante, chunks.get(0).text());
    }

    @Test
    void noGeneraFragmentosHechosSoloDeSolapamiento() {
        String text = String.join(" ", java.util.Collections.nCopies(120, "dato"));
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, text)));
        FixedSizeChunker chunker = new FixedSizeChunker(estimator, 20, 10);

        List<Chunk> chunks = chunker.split(document);

        assertTrue(chunks.size() > 1);
        String last = chunks.get(chunks.size() - 1).text();
        assertTrue(last.contains("dato"));
        assertFalse(last.isBlank());
    }

    @Test
    void unaPaginaSinPalabrasNoProduceFragmentos() {
        Document document = document(DocumentFormat.TXT, List.of(new DocumentPage(1, "   \n\t  ")));

        assertTrue(new FixedSizeChunker(estimator, 10, 0).split(document).isEmpty());
    }

    @Test
    void validaTamanoYSolapamiento() {
        assertThrows(IllegalArgumentException.class, () -> new FixedSizeChunker(estimator, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FixedSizeChunker(estimator, 10, -1));
        assertThrows(IllegalArgumentException.class, () -> new FixedSizeChunker(estimator, 10, 10));
        assertThrows(NullPointerException.class, () -> new FixedSizeChunker(null, 10, 2));
    }

    private static Document document(DocumentFormat format, List<DocumentPage> pages) {
        String extension = switch (format) {
            case TXT -> "txt";
            case MARKDOWN -> "md";
            case PDF -> "pdf";
        };
        return new Document(Path.of("docs", "prueba." + extension), "prueba", format, pages);
    }
}
