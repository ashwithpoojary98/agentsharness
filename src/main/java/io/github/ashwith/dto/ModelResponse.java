package io.github.ashwith.dto;

import java.util.List;

public record ModelResponse(String content, List<ToolCall> toolCalls, TokenUsage tokenUsage) {

    public static ModelResponse of(String content, List<ToolCall> toolCalls) {
        return new ModelResponse(content, toolCalls, TokenUsage.zero());
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
