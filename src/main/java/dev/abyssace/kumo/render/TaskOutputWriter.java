package dev.abyssace.kumo.render;

import dev.abyssace.kumo.summarize.SummaryException;
import dev.abyssace.kumo.task.TaskException;
import dev.abyssace.kumo.i18n.Messages;
import java.nio.file.Path;

/** Patrón: Adapter; reutiliza la publicación segura de texto de F4 para JSON y Markdown. */
public final class TaskOutputWriter {
    private final MarkdownSummaryWriter delegate = new MarkdownSummaryWriter();

    public void validate(Path output) {
        try {
            delegate.validate(output);
        } catch (SummaryException ex) {
            throw new TaskException(Messages.TASK_OUTPUT_ERROR.formatted(output, ex.getMessage()));
        }
    }

    public void write(Path output, String text) {
        try {
            delegate.write(output, text);
        } catch (SummaryException ex) {
            throw new TaskException(Messages.TASK_OUTPUT_ERROR.formatted(output, ex.getMessage()));
        }
    }
}
