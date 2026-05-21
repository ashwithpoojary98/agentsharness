package io.github.ashwith.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ashwith.config.AgentConfig;
import io.github.ashwith.context.Message;
import io.github.ashwith.dto.ModelResponse;
import io.github.ashwith.dto.ToolCall;
import io.github.ashwith.dto.TokenUsage;
import io.github.ashwith.tools.LLMTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OpenAiModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(OpenAiModel.class);
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiModel(AgentConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        log.info("OpenAiModel initialized — model: {}", config.getModelName());
    }

    @Override
    public ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools) {
        log.info("Invoking OpenAI '{}' — {} message(s), {} tool(s)", config.getModelName(), messages.size(), tools.size());
        try {
            String body = mapper.writeValueAsString(buildRequest(messages, tools));
            String baseUrl = config.getBaseUrl().isBlank() ? DEFAULT_BASE_URL : config.getBaseUrl();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("HTTP response status: {}", response.statusCode());

            ModelResponse result = parseResponse(response.body());
            log.info("OpenAI response — content: {} chars, tool calls: {}, tokens: {}",
                    result.content().length(), result.toolCalls().size(), result.tokenUsage());
            return result;
        } catch (Exception e) {
            log.error("OpenAI invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI invocation failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequest(List<Message> messages, Collection<LLMTool> tools) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.getModelName());
        root.put("temperature", config.getTemperature());

        var messagesArray = root.putArray("messages");
        for (Message msg : messages) {
            var msgNode = messagesArray.addObject();
            msgNode.put("role", msg.role());
            msgNode.put("content", msg.content() != null ? msg.content() : "");
        }

        if (!tools.isEmpty()) {
            var toolsArray = root.putArray("tools");
            for (LLMTool tool : tools) {
                var toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                var fn = toolNode.putObject("function");
                fn.put("name", tool.name());
                fn.put("description", tool.description());
                fn.set("parameters", mapper.valueToTree(tool.parameters()));
            }
        }

        return root;
    }

    private ModelResponse parseResponse(String body) throws Exception {
        JsonNode root = mapper.readTree(body);
        JsonNode message = root.path("choices").path(0).path("message");
        String content = message.path("content").asText("");
        List<ToolCall> toolCalls = new ArrayList<>();

        if (message.has("tool_calls")) {
            for (JsonNode tc : message.get("tool_calls")) {
                String name = tc.path("function").path("name").asText();
                String argsStr = tc.path("function").path("arguments").asText("{}");
                Map<String, Object> args = mapper.readValue(argsStr, new TypeReference<>() {});
                toolCalls.add(new ToolCall(name, args));
                log.debug("Parsed tool call: {}({})", name, args);
            }
        }

        JsonNode usage = root.path("usage");
        TokenUsage tokenUsage = new TokenUsage(
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                usage.path("total_tokens").asInt(0)
        );

        return new ModelResponse(content, toolCalls, tokenUsage);
    }
}
