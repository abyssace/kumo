package dev.abyssace.kumo.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.domain.Citation;
import dev.abyssace.kumo.domain.SourceLocation;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class WriteMarkdownRendererTest {
    private final Answer answer = new Answer("Redacta", "Texto [1].", List.of(new Citation(
            Path.of("input.pdf"), new SourceLocation.PageRef(3), 0, "Dato <script> con [texto]", 0.75)));

    @Test
    void anadeCoberturaFuentesPaginaYExtractoEscapado() {
        String markdown = new WriteMarkdownRenderer().render(answer, SummaryLanguage.SPANISH, true, 12);
        assertTrue(markdown.contains("Cobertura parcial"));
        assertTrue(markdown.contains("## Fuentes recuperadas"));
        assertTrue(markdown.contains("input.pdf"));
        assertTrue(markdown.contains("3"));
        assertTrue(markdown.contains("&lt;script&gt;"));
        assertTrue(markdown.contains("similitud 0.75"));
        assertFalse(markdown.contains("<script>"));
    }

    @Test
    void respetaMetadatosEnInglesYVerboseDesactivado() {
        String markdown = new WriteMarkdownRenderer().render(answer, SummaryLanguage.ENGLISH, false, 12);
        assertTrue(markdown.contains("Partial coverage"));
        assertTrue(markdown.contains("## Retrieved sources"));
        assertFalse(markdown.contains("similitud"));
    }
}
