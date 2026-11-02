package dev.abyssace.kumo.loader;

import java.util.Set;

/** Patrón: Template Method concreta. Lee código UTF-8 como texto, sin ejecutarlo. */
public final class SourceCodeDocumentLoader extends TextDocumentLoader {

    @Override
    protected Set<String> extensions() {
        return Set.of("java", "py", "js", "ts", "c", "cpp", "h", "cs", "go", "rs",
                "html", "css", "sql");
    }
}
