package dev.abyssace.kumo.fake;

import dev.abyssace.kumo.llm.LlmClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Patrón: Test Double (Stub/Spy) de {@link LlmClient}.
 *
 * <p>Devuelve siempre la misma respuesta y guarda los prompts recibidos para
 * poder verificar qué se le pidió al modelo. El streaming emite la respuesta
 * carácter a carácter para que la concatenación sea exacta.</p>
 */
public final class EchoLlmClient implements LlmClient {

    public static final String RESPONSE = "respuesta simulada";

    private final List<String> prompts = new ArrayList<>();

    @Override
    public String complete(String prompt) {
        prompts.add(Objects.requireNonNull(prompt, "prompt"));
        return RESPONSE;
    }

    @Override
    public void stream(String prompt, Consumer<String> onToken) {
        Objects.requireNonNull(onToken, "onToken");
        String response = complete(prompt);
        for (int i = 0; i < response.length(); i++) {
            onToken.accept(String.valueOf(response.charAt(i)));
        }
    }

    public List<String> prompts() {
        return List.copyOf(prompts);
    }

    public String lastPrompt() {
        if (prompts.isEmpty()) {
            throw new IllegalStateException("Todavía no se ha llamado al modelo");
        }
        return prompts.get(prompts.size() - 1);
    }
}
