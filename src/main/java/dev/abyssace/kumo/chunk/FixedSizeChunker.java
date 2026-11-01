package dev.abyssace.kumo.chunk;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.domain.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Strategy concreta + Template Method sobre las páginas.
 *
 * <p>Divide cada página en fragmentos de tamaño fijo estimado en tokens,
 * cortando siempre en límites de palabra para no partir palabras por la mitad.
 * Los fragmentos consecutivos comparten una cola de {@code overlapTokens}
 * tokens, de modo que una idea que cae en la frontera sigue siendo recuperable.
 * El solapamiento nunca genera un fragmento formado solo por texto repetido: el
 * último fragmento siempre aporta contenido nuevo.</p>
 *
 * <p>La ubicación citada respeta el formato: {@link SourceLocation.PageRef} en
 * PDF y {@link SourceLocation.LineRange} en TXT/Markdown. El índice del
 * fragmento es global al documento, no a la página.</p>
 */
public final class FixedSizeChunker implements Chunker {

    private final TokenEstimator estimator;
    private final int chunkSizeTokens;
    private final int overlapTokens;

    public FixedSizeChunker(TokenEstimator estimator, int chunkSizeTokens, int overlapTokens) {
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        if (chunkSizeTokens < 1) {
            throw new IllegalArgumentException("chunkSizeTokens debe ser >= 1, pero fue " + chunkSizeTokens);
        }
        if (overlapTokens < 0) {
            throw new IllegalArgumentException("overlapTokens debe ser >= 0, pero fue " + overlapTokens);
        }
        if (overlapTokens >= chunkSizeTokens) {
            throw new IllegalArgumentException(
                    "El solapamiento (" + overlapTokens + ") debe ser menor que el tamaño del fragmento ("
                            + chunkSizeTokens + ")");
        }
        this.chunkSizeTokens = chunkSizeTokens;
        this.overlapTokens = overlapTokens;
    }

    @Override
    public List<Chunk> split(Document document) {
        Objects.requireNonNull(document, "document");

        List<Chunk> chunks = new ArrayList<>();
        int lineOffset = 0;

        for (DocumentPage page : document.pages()) {
            List<Word> words = tokenize(page.text());
            if (!words.isEmpty()) {
                splitPage(document, page, words, lineOffset, chunks);
            }
            lineOffset += countLines(page.text());
        }

        return List.copyOf(chunks);
    }

    private void splitPage(
            Document document,
            DocumentPage page,
            List<Word> words,
            int lineOffset,
            List<Chunk> out) {

        String text = page.text();
        int start = 0;

        while (start < words.size()) {
            int end = start;
            while (end < words.size()) {
                String candidate = text.substring(words.get(start).start(), words.get(end).end());
                if (estimator.estimate(candidate) > chunkSizeTokens && end > start) {
                    break;
                }
                end++;
            }

            String chunkText = text.substring(words.get(start).start(), words.get(end - 1).end());
            out.add(Chunk.of(document.path(), out.size(), chunkText, location(document, page, words, start, end, lineOffset)));

            if (end == words.size()) {
                break;
            }
            start = nextStart(words, start, end);
        }
    }

    private SourceLocation location(
            Document document,
            DocumentPage page,
            List<Word> words,
            int start,
            int end,
            int lineOffset) {

        if (document.format() == DocumentFormat.PDF) {
            return new SourceLocation.PageRef(page.number());
        }
        int startLine = words.get(start).line() + lineOffset;
        int endLine = words.get(end - 1).line() + lineOffset;
        return new SourceLocation.LineRange(startLine, endLine);
    }

    /**
     * Calcula dónde empieza el fragmento siguiente: retrocede desde {@code end}
     * tantas palabras como quepan en el solapamiento, sin llegar nunca a
     * {@code start} (eso impediría avanzar).
     */
    private int nextStart(List<Word> words, int start, int end) {
        if (overlapTokens == 0) {
            return end;
        }

        int tokens = 0;
        int candidate = end;
        for (int i = end - 1; i > start; i--) {
            int wordTokens = estimator.estimate(words.get(i).text()) + 1; // +1 por el separador
            if (tokens + wordTokens > overlapTokens) {
                break;
            }
            tokens += wordTokens;
            candidate = i;
        }
        return Math.max(candidate, start + 1);
    }

    private static List<Word> tokenize(String text) {
        List<Word> words = new ArrayList<>();
        int line = 1;
        int i = 0;
        int length = text.length();

        while (i < length) {
            char current = text.charAt(i);
            if (current == '\n') {
                line++;
                i++;
                continue;
            }
            if (Character.isWhitespace(current)) {
                i++;
                continue;
            }

            int start = i;
            while (i < length && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            words.add(new Word(text.substring(start, i), line, start, i));
        }
        return words;
    }

    private static int countLines(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    /** Palabra con su línea (1-based) y sus desplazamientos en la página. */
    private record Word(String text, int line, int start, int end) {
    }
}
