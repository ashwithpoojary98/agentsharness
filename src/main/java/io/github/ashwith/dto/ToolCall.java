package io.github.ashwith.dto;

import java.util.Map;

public record ToolCall(String toolName, Map<String, Object> arguments) {}
