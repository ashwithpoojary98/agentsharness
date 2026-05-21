package io.github.ashwith.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ashwith.config.AgentConfig;
import io.github.ashwith.context.Message;
import io.github.ashwith.dto.ModelResponse;
import io.github.ashwith.dto.ToolCall;
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

public class GeminiModel implements Model {

    private static final Logger log = LoggerFactory.getLogger(GeminiModel.class);

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public GeminiModel(AgentConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        log.info("GeminiModel initialized — model: {}", config.getModelName());
    }

    @Override
    public ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools) {
        log.info("Invoking Gemini '{}' — {} message(s), {} tool(s)", config.getModelName(), messages.size(), tools.size());
        try {
            String body = mapper.writeValueAsString(buildRequest(messages, tools));
            String url = API_BASE + config.getModelName() + ":generateContent?key=" + config.getApiKey();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("HTTP response status: {}", response.statusCode());

            ModelResponse result = parseResponse(response.body());
            log.info("Gemini response — content length: {}, tool calls: {}",
                    result.content().length(), result.toolCalls().size());
            return result;
        } catch (Exception e) {
            log.error("Gemini invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini invocation failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequest(List<Message> messages, Collection<LLMTool> tools) {
        ObjectNode root = mapper.createObjectNode();
        var contentsArray = root.putArray("contents");

        messages.stream()
                .filter(m -> "system".equals(m.role()))
                .findFirst()
                .ifPresent(sys -> root.putObject("system_instruction")
                        .putArray("parts").addObject().put("text", sys.content()));

        for (Message msg : messages) {
            if ("system".equals(msg.role())) continue;
            var contentNode = contentsArray.addObject();
            contentNode.put("role", "user".equals(msg.role()) ? "user" : "model");
            var partsArray = contentNode.putArray("parts");
            partsArray.addObject().put("text", msg.content());
        }

        if (!tools.isEmpty()) {
            var fnDeclarations = root.putArray("tools").addObject().putArray("function_declarations");
            for (LLMTool tool : tools) {
                var fn = fnDeclarations.addObject();
                fn.put("name", tool.name());
                fn.put("description", tool.description());
                fn.set("parameters", mapper.valueToTree(tool.parameters()));
            }
        }

        return root;
    }

    private ModelResponse parseResponse(String body) throws Exception {
        JsonNode root = mapper.readTree(body);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        String content = "";
        List<ToolCall> toolCalls = new ArrayList<>();

        for (JsonNode part : parts) {
            if (part.has("text")) {
                content = part.get("text").asText("");
            } else if (part.has("functionCall")) {
                JsonNode fc = part.get("functionCall");
                String name = fc.get("name").asText();
                Map<String, Object> args = mapper.convertValue(fc.get("args"), new TypeReference<>() {});
                toolCalls.add(new ToolCall(name, args));
                log.debug("Parsed tool call: {}({})", name, args);
            }
        }

        return new ModelResponse(content, toolCalls);
    }
}
