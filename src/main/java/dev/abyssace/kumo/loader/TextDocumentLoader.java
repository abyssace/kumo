package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Patrón: Template Method concreta para texto plano.
 *
 * <p>Un TXT es una sola página lógica. El chunker se encarga después de
 * calcular los rangos de líneas reales para las citas.</p>
 */
public class TextDocumentLoader extends AbstractDocumentLoader {

    @Override
    protected Set<String> extensions() {
        return Set.of("txt");
    }

    @Override
    protected DocumentFormat format() {
        return DocumentFormat.TXT;
    }

    @Override
    protected List<DocumentPage> readPages(Path path) throws IOException {
        String text = stripBom(Files.readString(path, StandardCharsets.UTF_8));
        if (text.isBlank()) {
            throw new DocumentLoadException("El archivo está vacío: " + path);
        }
        return List.of(new DocumentPage(1, text));
    }

    /** Quita el BOM UTF-8 si el editor lo añadió. */
    protected static String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}
