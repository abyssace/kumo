package dev.abyssace.kumo.llm;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Patrón: Utility / Decorator de texto.
 *
 * <p>Elimina los bloques de razonamiento que algunos modelos (Qwen3) emiten
 * entre las etiquetas de apertura y cierre. Es la red de seguridad para las
 * respuestas completas; en streaming se usa {@link ReasoningFilter}. Si el
 * bloque queda abierto, se descarta todo lo que sigue a la etiqueta de
 * apertura.</p>
 */
public final class ReasoningStripper {

    // \u003c y \u003e son '<' y '>'; así el patrón conserva siempre la
    // etiqueta completa independientemente de cómo se guarde el archivo.
    private static final Pattern COMPLETE_BLOCKS =
            Pattern.compile("(?is)\u003cthinking\u003e.*?\u003c/thinking\u003e");
    private static final List<String> OPEN_TAGS = List.of("\u003cthinking\u003e");

    private ReasoningStripper() {
    }

    /** Devuelve el texto sin razonamiento, recortado en los extremos. */
    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String result = COMPLETE_BLOCKS.matcher(text).replaceAll("");
        for (String tag : OPEN_TAGS) {
            int open = result.indexOf(tag);
            if (open >= 0) {
                result = result.substring(0, open);
            }
        }
        return result.strip();
    }
}
