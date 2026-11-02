package dev.abyssace.kumo.llm;

import java.util.List;

/**
 * Patrón: State Machine + Decorator de stream.
 *
 * <p>Filtra los bloques de razonamiento de Qwen3 aunque las etiquetas lleguen
 * partidas entre tokens. En estado normal retiene el sufijo que podría ser el
 * comienzo de una etiqueta; en estado de razonamiento descarta el contenido
 * hasta el cierre. Al terminar, si el bloque quedó abierto, no emite nada de
 * él.</p>
 *
 * <p>No es seguro para uso concurrente: cada stream tiene su propia instancia.</p>
 */
public final class ReasoningFilter {

    private record TagPair(String open, String close) {
    }

    // \u003c y \u003e son '<' y '>'. Se escriben así para que el compilador
    // reciba siempre la etiqueta completa, sin depender de cómo se guarde el
    // archivo fuente.
    private static final List<TagPair> TAGS = List.of(
            new TagPair("\u003cthinking\u003e", "\u003c/thinking\u003e"));

    private final StringBuilder pending = new StringBuilder();
    private TagPair active;
    private boolean finished;

    /** Añade un trozo del stream y devuelve el texto seguro para mostrar. */
    public String feed(String chunk) {
        if (finished) {
            throw new IllegalStateException("El filtro ya se cerró con finish()");
        }
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        pending.append(chunk);
        StringBuilder out = new StringBuilder();

        while (true) {
            if (active == null) {
                int openIndex = -1;
                TagPair openTag = null;
                for (TagPair tag : TAGS) {
                    int index = pending.indexOf(tag.open());
                    if (index >= 0 && (openIndex == -1 || index < openIndex)) {
                        openIndex = index;
                        openTag = tag;
                    }
                }
                if (openTag != null) {
                    out.append(pending, 0, openIndex);
                    pending.delete(0, openIndex + openTag.open().length());
                    active = openTag;
                    continue;
                }

                int keep = longestSuffixThatIsPrefix(
                        pending, TAGS.stream().map(TagPair::open).toList());
                int emit = pending.length() - keep;
                if (emit > 0) {
                    out.append(pending, 0, emit);
                    pending.delete(0, emit);
                }
                break;
            }

            int closeIndex = pending.indexOf(active.close());
            if (closeIndex >= 0) {
                pending.delete(0, closeIndex + active.close().length());
                active = null;
                continue;
            }

            int keep = longestSuffixThatIsPrefix(pending, List.of(active.close()));
            int discard = pending.length() - keep;
            if (discard > 0) {
                pending.delete(0, discard);
            }
            break;
        }

        return out.toString();
    }

    /** Cierra el stream y devuelve el texto pendiente que sea seguro. */
    public String finish() {
        if (finished) {
            return "";
        }
        finished = true;
        if (active != null) {
            pending.setLength(0);
            return "";
        }
        String out = pending.toString();
        pending.setLength(0);
        return out;
    }

    private static int longestSuffixThatIsPrefix(CharSequence text, List<String> tags) {
        int max = 0;
        for (String tag : tags) {
            int limit = Math.min(text.length(), tag.length() - 1);
            for (int length = limit; length > max; length--) {
                if (matchesSuffix(text, tag, length)) {
                    max = length;
                    break;
                }
            }
        }
        return max;
    }

    private static boolean matchesSuffix(CharSequence text, String tag, int length) {
        int offset = text.length() - length;
        for (int i = 0; i < length; i++) {
            if (text.charAt(offset + i) != tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
