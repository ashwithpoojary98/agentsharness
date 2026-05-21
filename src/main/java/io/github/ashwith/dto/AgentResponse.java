package io.github.ashwith.dto;

import java.util.List;

public record AgentResponse(String content, List<ToolCall> toolCallTrace, boolean blocked, String blockReason, TokenUsage tokenUsage) {

    public static AgentResponse of(String content, List<ToolCall> trace, TokenUsage usage) {
        return new AgentResponse(content, trace, false, null, usage);
    }

    public static AgentResponse blocked(String reason) {
        return new AgentResponse(null, List.of(), true, reason, TokenUsage.zero());
    }

    public static AgentResponse directResponse(String content) {
        return new AgentResponse(content, List.of(), false, null, TokenUsage.zero());
    }
}
