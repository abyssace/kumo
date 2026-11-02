package dev.abyssace.kumo.task;

/** Patrón: excepción de dominio para extracción y redacción fallidas. */
public final class TaskException extends RuntimeException {
    public TaskException(String message) {
        super(message);
    }
}
