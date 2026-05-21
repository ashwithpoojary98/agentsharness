package io.github.ashwith.context;

import io.github.ashwith.dto.ToolCall;

import java.util.List;

public record Message(String role, String content, List<ToolCall> toolCalls) {

    public static Message user(String content) {
        return new Message("user", content, null);
    }

    public static Message assistant(String content) {
        return new Message("assistant", content, null);
    }

    public static Message assistantWithToolCalls(List<ToolCall> toolCalls) {
        return new Message("assistant", "", toolCalls);
    }

    public static Message system(String content) {
        return new Message("system", content, null);
    }

    public static Message tool(String content) {
        return new Message("tool", content, null);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
