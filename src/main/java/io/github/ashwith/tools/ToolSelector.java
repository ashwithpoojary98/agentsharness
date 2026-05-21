package io.github.ashwith.tools;

import io.github.ashwith.dto.ToolCall;

import java.util.Optional;

public class ToolSelector {

    private final ToolRegistry registry;

    public ToolSelector(ToolRegistry registry) {
        this.registry = registry;
    }

    public Optional<LLMTool> select(ToolCall call) {
        return registry.getTool(call.toolName());
    }
}
