package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.DocumentFormat;
import java.util.Set;

/**
 * Patrón: Template Method concreta (hereda la lectura de texto).
 *
 * <p>Markdown se trata como texto plano por ahora: el valor está en el
 * contenido, no en renderizar el formato. En una fase posterior se podría
 * limpiar la sintaxis antes de embeber.</p>
 */
public final class MarkdownDocumentLoader extends TextDocumentLoader {

    @Override
    protected Set<String> extensions() {
        return Set.of("md", "markdown");
    }

    @Override
    protected DocumentFormat format() {
        return DocumentFormat.MARKDOWN;
    }
}
