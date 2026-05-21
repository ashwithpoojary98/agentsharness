package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;

public sealed interface GuardrailResult
        permits GuardrailResult.Pass, GuardrailResult.Block, GuardrailResult.DirectResponse {

    record Pass(AgentRequest request) implements GuardrailResult {}

    record Block(String reason) implements GuardrailResult {}

    record DirectResponse(String response) implements GuardrailResult {}
}
