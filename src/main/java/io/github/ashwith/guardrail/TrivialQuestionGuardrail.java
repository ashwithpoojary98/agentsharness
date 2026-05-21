package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class TrivialQuestionGuardrail implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(TrivialQuestionGuardrail.class);

    private static final Set<String> GREETINGS = Set.of(
            "hi", "hello", "hey", "good morning", "good evening", "good night",
            "how are you", "what's up", "whats up", "sup", "yo", "howdy", "hiya"
    );

    @Override
    public GuardrailResult check(AgentRequest request) {
        String input = request.userInput().trim().toLowerCase();
        log.debug("Checking for trivial input: '{}'", input);

        if (GREETINGS.contains(input)) {
            log.info("Greeting detected — responding directly without LLM call");
            return new GuardrailResult.DirectResponse("Hello! How can I help you today?");
        }

        String[] words = input.split("\\s+");
        if (words.length == 1 && input.length() < 8 && !input.endsWith("?")) {
            log.info("Single vague word '{}' detected — responding directly without LLM call", input);
            return new GuardrailResult.DirectResponse("Could you give me more detail? I'm here to help.");
        }

        log.debug("Input is non-trivial — passing through");
        return new GuardrailResult.Pass(request);
    }
}
