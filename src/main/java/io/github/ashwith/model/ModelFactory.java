package io.github.ashwith.model;

import io.github.ashwith.config.AgentConfig;

public final class ModelFactory {

    private ModelFactory() {}

    public static Model create(AgentConfig config) {
        return switch (config.getModelType()) {
            case OLLAMA -> new OllamaModel(config);
            case GEMINI -> new GeminiModel(config);
            case OPENAI -> new OpenAiModel(config);
        };
    }
}
