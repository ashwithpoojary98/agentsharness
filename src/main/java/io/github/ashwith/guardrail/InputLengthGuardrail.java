package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputLengthGuardrail implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(InputLengthGuardrail.class);

    private final int maxLength;

    public InputLengthGuardrail(int maxLength) {
        this.maxLength = maxLength;
        log.debug("Configured with maxLength={}", maxLength);
    }

    @Override
    public GuardrailResult check(AgentRequest request) {
        if (request.userInput() == null || request.userInput().isBlank()) {
            log.warn("Empty input rejected");
            return new GuardrailResult.Block("Input cannot be empty.");
        }

        int length = request.userInput().length();
        log.debug("Input length: {} chars (max: {})", length, maxLength);

        if (length > maxLength) {
            log.warn("Input too long — {} chars exceeds limit of {}", length, maxLength);
            return new GuardrailResult.Block("Input exceeds maximum allowed length of " + maxLength + " characters.");
        }

        return new GuardrailResult.Pass(request);
    }
}
