package io.github.ashwith.dto;

import java.util.List;

public record ModelResponse(String content, List<ToolCall> toolCalls) {
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
