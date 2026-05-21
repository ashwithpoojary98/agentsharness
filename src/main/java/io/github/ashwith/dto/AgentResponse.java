package io.github.ashwith.dto;

import java.util.List;

public record AgentResponse(String content, List<ToolCall> toolCallTrace, boolean blocked, String blockReason) {

    public static AgentResponse of(String content, List<ToolCall> trace) {
        return new AgentResponse(content, trace, false, null);
    }

    public static AgentResponse blocked(String reason) {
        return new AgentResponse(null, List.of(), true, reason);
    }

    public static AgentResponse directResponse(String content) {
        return new AgentResponse(content, List.of(), false, null);
    }
}
