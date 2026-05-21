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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OllamaModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(OllamaModel.class);

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OllamaModel(AgentConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        log.info("OllamaModel initialized — endpoint: {}, model: {}", config.getBaseUrl(), config.getModelName());
    }

    @Override
    public ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools) {
        log.info("Invoking Ollama '{}' — {} message(s), {} tool(s)", config.getModelName(), messages.size(), tools.size());
        try {
            String body = mapper.writeValueAsString(buildRequest(messages, tools, false));
            log.debug("Request payload size: {} bytes", body.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("HTTP response status: {}", response.statusCode());

            ModelResponse result = parseResponse(response.body());
            log.info("Ollama response — content: {} chars, tool calls: {}, tokens: {}",
                    result.content().length(), result.toolCalls().size(), result.tokenUsage());
            return result;
        } catch (Exception e) {
            log.error("Ollama invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama invocation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools, Consumer<String> onToken) {
        log.info("Invoking Ollama '{}' streaming — {} message(s), {} tool(s)", config.getModelName(), messages.size(), tools.size());
        try {
            String body = mapper.writeValueAsString(buildRequest(messages, tools, true));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            log.debug("HTTP streaming response status: {}", response.statusCode());

            ModelResponse result = parseStreamingResponse(response.body(), onToken);
            log.info("Ollama stream complete — content: {} chars, tool calls: {}, tokens: {}",
                    result.content().length(), result.toolCalls().size(), result.tokenUsage());
            return result;
        } catch (Exception e) {
            log.error("Ollama streaming invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama streaming invocation failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequest(List<Message> messages, Collection<LLMTool> tools, boolean stream) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.getModelName());
        root.put("stream", stream);
        root.putObject("options").put("temperature", config.getTemperature());

        var messagesArray = root.putArray("messages");
        for (Message msg : messages) {
            var msgNode = messagesArray.addObject();
            msgNode.put("role", msg.role());
            if (msg.hasToolCalls()) {
                msgNode.put("content", "");
                var tcArray = msgNode.putArray("tool_calls");
                for (ToolCall tc : msg.toolCalls()) {
                    var fn = tcArray.addObject().putObject("function");
                    fn.put("name", tc.toolName());
                    fn.set("arguments", mapper.valueToTree(tc.arguments()));
                }
            } else {
                msgNode.put("content", msg.content());
            }
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
        JsonNode message = root.get("message");
        String content = message.path("content").asText("");
        List<ToolCall> toolCalls = parseToolCalls(message);

        int promptTokens = root.path("prompt_eval_count").asInt(0);
        int completionTokens = root.path("eval_count").asInt(0);
        TokenUsage tokenUsage = new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);

        return new ModelResponse(content, toolCalls, tokenUsage);
    }

    private ModelResponse parseStreamingResponse(Stream<String> lines, Consumer<String> onToken) {
        StringBuilder contentBuilder = new StringBuilder();
        AtomicReference<List<ToolCall>> toolCallsRef = new AtomicReference<>(new ArrayList<>());
        AtomicReference<TokenUsage> usageRef = new AtomicReference<>(TokenUsage.zero());

        lines.filter(line -> !line.isBlank()).forEach(line -> {
            try {
                JsonNode chunk = mapper.readTree(line);
                JsonNode message = chunk.path("message");

                String token = message.path("content").asText("");
                if (!token.isEmpty()) {
                    contentBuilder.append(token);
                    onToken.accept(token);
                }

                if (message.has("tool_calls")) {
                    toolCallsRef.set(parseToolCalls(message));
                }

                if (chunk.path("done").asBoolean(false)) {
                    int promptTokens = chunk.path("prompt_eval_count").asInt(0);
                    int completionTokens = chunk.path("eval_count").asInt(0);
                    usageRef.set(new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens));
                    log.debug("Stream done — tokens: {}", usageRef.get());
                }
            } catch (Exception e) {
                log.warn("Failed to parse streaming chunk: {}", line);
            }
        });

        return new ModelResponse(contentBuilder.toString(), toolCallsRef.get(), usageRef.get());
    }

    private List<ToolCall> parseToolCalls(JsonNode message) throws Exception {
        List<ToolCall> toolCalls = new ArrayList<>();
        if (message.has("tool_calls")) {
            for (JsonNode tc : message.get("tool_calls")) {
                JsonNode fn = tc.get("function");
                String name = fn.get("name").asText();
                Map<String, Object> args = mapper.convertValue(fn.get("arguments"), new TypeReference<>() {});
                toolCalls.add(new ToolCall(name, args));
                log.debug("Parsed tool call: {}({})", name, args);
            }
        }
        return toolCalls;
    }
}
