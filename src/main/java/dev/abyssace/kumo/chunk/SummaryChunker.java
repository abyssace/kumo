package dev.abyssace.kumo.chunk;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Patrón: Strategy de Chunker para resumen: sin solapamiento, conserva sangría y páginas. */
public final class SummaryChunker implements Chunker {
    private final BudgetTextSplitter splitter;

    public SummaryChunker(TokenEstimator estimator, int budget) {
        splitter = new BudgetTextSplitter(estimator, budget);
    }

    @Override
    public List<Chunk> split(Document document) {
        List<Chunk> result = new ArrayList<>();
        int line = 1;
        for (var page : document.pages()) {
            for (String part : splitter.split(page.text())) {
                int breaks = (int) part.chars().filter(c -> c == '\n').count();
                if (!part.isBlank()) {
                    SourceLocation location = document.format() == DocumentFormat.PDF
                            ? new SourceLocation.PageRef(page.number())
                            : new SourceLocation.LineRange(line, line + breaks);
                    result.add(Chunk.of(document.path(), result.size(), part, location));
                }
                line += breaks;
            }
            line++;
        }
        return List.copyOf(result);
    }
}
