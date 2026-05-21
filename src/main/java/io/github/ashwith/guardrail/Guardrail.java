package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;

@FunctionalInterface
public interface Guardrail {
    GuardrailResult check(AgentRequest request);
}
