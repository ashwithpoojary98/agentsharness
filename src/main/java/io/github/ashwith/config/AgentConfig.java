package io.github.ashwith.config;

public class AgentConfig {

    private final ModelType modelType;
    private final String modelName;
    private final String apiKey;
    private final String baseUrl;
    private final int maxIterations;
    private final int maxContextMessages;
    private final String systemPrompt;
    private final double temperature;

    private AgentConfig(Builder builder) {
        this.modelType = builder.modelType;
        this.modelName = builder.modelName;
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.maxIterations = builder.maxIterations;
        this.maxContextMessages = builder.maxContextMessages;
        this.systemPrompt = builder.systemPrompt;
        this.temperature = builder.temperature;
    }

    public ModelType getModelType() { return modelType; }
    public String getModelName() { return modelName; }
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public int getMaxIterations() { return maxIterations; }
    public int getMaxContextMessages() { return maxContextMessages; }
    public String getSystemPrompt() { return systemPrompt; }
    public double getTemperature() { return temperature; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ModelType modelType = ModelType.OLLAMA;
        private String modelName = "llama3.2";
        private String apiKey = "";
        private String baseUrl = "http://localhost:11434";
        private int maxIterations = 10;
        private int maxContextMessages = 20;
        private String systemPrompt = "You are a helpful assistant.";
        private double temperature = 0.7;

        public Builder modelType(ModelType modelType) { this.modelType = modelType; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder maxIterations(int maxIterations) { this.maxIterations = maxIterations; return this; }
        public Builder maxContextMessages(int maxContextMessages) { this.maxContextMessages = maxContextMessages; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }

        public AgentConfig build() {
            if (apiKey.isBlank()) {
                String envKey = switch (modelType) {
                    case GEMINI -> System.getenv("GEMINI_API_KEY");
                    case OPENAI -> System.getenv("OPENAI_API_KEY");
                    default -> null;
                };
                if (envKey != null && !envKey.isBlank()) this.apiKey = envKey;
            }
            return new AgentConfig(this);
        }
    }
}
