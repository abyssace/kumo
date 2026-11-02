package dev.abyssace.kumo.summarize;

/** Patrón: excepción de dominio para un resumen que no puede completarse con seguridad. */
public final class SummaryException extends RuntimeException {
    public SummaryException(String message) {
        super(message);
    }
}
