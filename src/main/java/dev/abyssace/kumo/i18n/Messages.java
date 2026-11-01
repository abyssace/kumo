package dev.abyssace.kumo.i18n;

/**
 * Patrón: Constant Holder.
 *
 * <p>Todos los textos visibles para el usuario viven aquí (en español) para
 * poder revisarlos y traducirlos en un solo sitio. Los prompts del modelo no
 * cuentan como mensajes de interfaz: viven en {@code prompt}.</p>
 */
public final class Messages {

    public static final String NO_CONTEXT =
            "No encontré fragmentos relevantes en el índice para responder a esa pregunta.";

    private Messages() {
        // Clase de constantes.
    }
}
