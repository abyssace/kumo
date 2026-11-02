package dev.abyssace.kumo.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.domain.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SummaryChunkerTest {
    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();

    @Test
    void conservaCodigoSangriaYTokensLexicosLargos() {
        String text = "class Test {\n    String x = \"" + "a".repeat(1200) + "\";\n}\n";
        var chunks = new SummaryChunker(estimator, 50).split(document(DocumentFormat.TXT,
                List.of(new DocumentPage(1, text))));
        assertEquals(text, chunks.stream().map(c -> c.text()).collect(Collectors.joining()));
        assertTrue(chunks.stream().allMatch(c -> estimator.estimate(c.text()) <= 50));
    }

    @Test
    void mantieneLasPaginasPdfSinMezclarlas() {
        var chunks = new SummaryChunker(estimator, 10).split(document(DocumentFormat.PDF,
                List.of(new DocumentPage(2, "primera página"), new DocumentPage(5, "segunda página"))));
        assertEquals(new SourceLocation.PageRef(2), chunks.get(0).location());
        assertEquals(new SourceLocation.PageRef(5), chunks.get(1).location());
    }

    @Test
    void splitterNoPierdeEspaciosYRespetaUnicode() {
        String text = "  a😀b  \n".repeat(200);
        List<String> parts = new BudgetTextSplitter(estimator, 8).split(text);
        assertEquals(text, String.join("", parts));
        assertTrue(parts.stream().allMatch(p -> estimator.estimate(p) <= 8));
        assertTrue(parts.stream().noneMatch(p -> Character.isHighSurrogate(p.charAt(p.length() - 1))));
    }

    @Test
    void splitterVacioYValidacionDelPresupuesto() {
        assertTrue(new BudgetTextSplitter(estimator, 1).split("").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new BudgetTextSplitter(estimator, 0));
    }

    private Document document(DocumentFormat format, List<DocumentPage> pages) {
        return new Document(Path.of("test.txt"), "test", format, pages);
    }
}
