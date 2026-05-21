package io.github.ashwith.tools;

import java.util.Map;

public interface LLMTool {
    String name();
    String description();
    Map<String, Object> parameters();
    String execute(String argumentsJson);
}
