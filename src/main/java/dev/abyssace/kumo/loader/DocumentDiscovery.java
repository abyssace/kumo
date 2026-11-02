package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.task.TaskException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Patrón: Visitor de descubrimiento; no sigue enlaces simbólicos ni entra en índices o builds. */
public final class DocumentDiscovery {
    private static final Set<String> IGNORED = Set.of(".kumo", ".git", "target", "build", "dist", "node_modules");

    public List<Path> discover(Path input, DocumentLoaderFactory loaders) {
        Path root = input.toAbsolutePath().normalize();
        if (Files.isRegularFile(root, LinkOption.NOFOLLOW_LINKS)) {
            loaders.loaderFor(root);
            return List.of(root);
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new TaskException(Messages.FILE_NOT_FOUND.formatted(root));
        }
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                    return !dir.equals(root) && IGNORED.contains(dir.getFileName().toString())
                            ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile() && loaders.supports(file)) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new DocumentLoadException(Messages.DOCUMENT_DISCOVERY_ERROR.formatted(root, ex.getMessage()), ex);
        }
        files.sort(Comparator.comparing(Path::toString));
        if (files.isEmpty()) {
            throw new TaskException(Messages.BRIEF_NO_FILES.formatted(root));
        }
        return List.copyOf(files);
    }
}
