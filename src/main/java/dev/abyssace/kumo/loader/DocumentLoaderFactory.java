package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.Document;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Patrón: Factory.
 *
 * <p>Selecciona el loader adecuado por extensión. Añadir DOCX en el futuro solo
 * requiere un loader nuevo y registrarlo aquí; ni el chunker ni el RAG cambian.</p>
 */
public final class DocumentLoaderFactory {

    private final List<DocumentLoader> loaders;

    public DocumentLoaderFactory() {
        this(List.of(
                new TextDocumentLoader(),
                new MarkdownDocumentLoader(),
                new PdfDocumentLoader()));
    }

    public DocumentLoaderFactory(List<DocumentLoader> loaders) {
        Objects.requireNonNull(loaders, "loaders");
        if (loaders.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos un loader registrado");
        }
        loaders.forEach(loader -> Objects.requireNonNull(loader, "loader"));
        this.loaders = List.copyOf(loaders);
    }

    public boolean supports(Path path) {
        Objects.requireNonNull(path, "path");
        return loaders.stream().anyMatch(loader -> loader.supports(path));
    }

    public DocumentLoader loaderFor(Path path) {
        Objects.requireNonNull(path, "path");
        return loaders.stream()
                .filter(loader -> loader.supports(path))
                .findFirst()
                .orElseThrow(() -> new DocumentLoadException(
                        "Formato no soportado: " + path + ". Extensiones disponibles: "
                                + String.join(", ", supportedExtensions())));
    }

    public Document load(Path path) {
        return loaderFor(path).load(path);
    }

    public Set<String> supportedExtensions() {
        Set<String> extensions = new TreeSet<>();
        for (DocumentLoader loader : loaders) {
            if (loader instanceof AbstractDocumentLoader abstractLoader) {
                extensions.addAll(abstractLoader.extensions());
            }
        }
        return Collections.unmodifiableSet(extensions);
    }
}
