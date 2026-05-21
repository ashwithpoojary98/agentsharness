package io.github.ashwith.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ashwith.dto.ToolCall;
import io.github.ashwith.dto.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final int maxRetries;

    public ToolExecutor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public ToolResult execute(LLMTool tool, ToolCall call) {
        log.info("Executing tool '{}' — args: {}", tool.name(), call.arguments());
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String argsJson = mapper.writeValueAsString(call.arguments());
                String result = tool.execute(argsJson);
                log.info("Tool '{}' succeeded on attempt {}/{}", tool.name(), attempt, maxRetries);
                log.debug("Tool '{}' result: {}", tool.name(), result);
                return new ToolResult(tool.name(), result, true);
            } catch (Exception e) {
                lastException = e;
                log.warn("Tool '{}' failed on attempt {}/{}: {}", tool.name(), attempt, maxRetries, e.getMessage());
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "unknown error";
        log.error("Tool '{}' exhausted all {} retries — final error: {}", tool.name(), maxRetries, errorMsg);
        return new ToolResult(tool.name(), "Failed after " + maxRetries + " attempts: " + errorMsg, false);
    }
}
