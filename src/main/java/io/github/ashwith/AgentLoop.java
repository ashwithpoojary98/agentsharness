package io.github.ashwith;

import io.github.ashwith.context.ContextManager;
import io.github.ashwith.dto.*;
import io.github.ashwith.guardrail.GuardrailChain;
import io.github.ashwith.model.Model;
import io.github.ashwith.tools.LLMTool;
import io.github.ashwith.tools.ToolExecutor;
import io.github.ashwith.tools.ToolRegistry;
import io.github.ashwith.tools.ToolSelector;
import io.github.ashwith.verification.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final Model model;
    private final ContextManager contextManager;
    private final GuardrailChain guardrailChain;
    private final ToolRegistry toolRegistry;
    private final ToolSelector toolSelector;
    private final ToolExecutor toolExecutor;
    private final Verifier verifier;
    private final int maxIterations;
    private final ExecutorService toolExecutorPool = Executors.newVirtualThreadPerTaskExecutor();

    public AgentLoop(Model model, ContextManager contextManager, GuardrailChain guardrailChain,
                     ToolRegistry toolRegistry, ToolSelector toolSelector, ToolExecutor toolExecutor,
                     Verifier verifier, int maxIterations) {
        this.model = model;
        this.contextManager = contextManager;
        this.guardrailChain = guardrailChain;
        this.toolRegistry = toolRegistry;
        this.toolSelector = toolSelector;
        this.toolExecutor = toolExecutor;
        this.verifier = verifier;
        this.maxIterations = maxIterations;
    }

    public AgentResponse run(AgentRequest request) {
        return run(request, token -> {});
    }

    public AgentResponse run(AgentRequest request, Consumer<String> onToken) {
        log.info("──── Agent run started ────────────────────────────");
        log.info("Input : '{}'", request.userInput());

        Optional<AgentResponse> earlyExit = guardrailChain.process(request);
        if (earlyExit.isPresent()) {
            AgentResponse response = earlyExit.get();
            if (response.blocked()) {
                log.warn("Run ended — blocked by guardrail: {}", response.blockReason());
            } else {
                log.info("Run ended — guardrail direct response (no LLM call)");
            }
            return response;
        }

        contextManager.addUserMessage(request.userInput());

        List<ToolCall> toolCallTrace = new ArrayList<>();
        List<LLMTool> tools = new ArrayList<>(toolRegistry.allTools());
        TokenUsage totalUsage = TokenUsage.zero();

        log.debug("Available tools: {}", tools.stream().map(LLMTool::name).toList());

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            log.info("Iteration [{}/{}] — invoking model", iteration + 1, maxIterations);

            ModelResponse modelResponse = model.invoke(contextManager.getMessages(), tools, onToken);
            totalUsage = totalUsage.add(modelResponse.tokenUsage());

            if (!modelResponse.hasToolCalls()) {
                log.info("Model returned final response ({} chars) — tokens this run: {}",
                        modelResponse.content().length(), totalUsage);
                contextManager.addAssistantMessage(modelResponse.content());
                return AgentResponse.of(modelResponse.content(), toolCallTrace, totalUsage);
            }

            log.info("Model requested {} tool call(s): {}",
                    modelResponse.toolCalls().size(),
                    modelResponse.toolCalls().stream().map(ToolCall::toolName).toList());

            contextManager.addAssistantToolCalls(modelResponse.toolCalls());
            toolCallTrace.addAll(modelResponse.toolCalls());

            if (modelResponse.toolCalls().size() == 1) {
                ToolCall toolCall = modelResponse.toolCalls().get(0);
                log.debug("Dispatching tool '{}' with args: {}", toolCall.toolName(), toolCall.arguments());
                ToolResult verified = verifier.verify(
                        toolSelector.select(toolCall)
                                .map(tool -> toolExecutor.execute(tool, toolCall))
                                .orElseGet(() -> {
                                    log.warn("Tool '{}' not found in registry", toolCall.toolName());
                                    return new ToolResult(toolCall.toolName(), "Tool not found: " + toolCall.toolName(), false);
                                })
                );
                log.debug("Tool '{}' result [success={}]: {}", verified.toolName(), verified.success(), verified.result());
                contextManager.addToolResult(verified.toolName(), verified.result());
            } else {
                log.info("Running {} tool calls in parallel", modelResponse.toolCalls().size());
                List<CompletableFuture<ToolResult>> futures = modelResponse.toolCalls().stream()
                        .map(toolCall -> CompletableFuture.supplyAsync(() -> {
                            log.debug("Dispatching tool '{}' with args: {}", toolCall.toolName(), toolCall.arguments());
                            return verifier.verify(
                                    toolSelector.select(toolCall)
                                            .map(tool -> toolExecutor.execute(tool, toolCall))
                                            .orElseGet(() -> {
                                                log.warn("Tool '{}' not found in registry", toolCall.toolName());
                                                return new ToolResult(toolCall.toolName(), "Tool not found: " + toolCall.toolName(), false);
                                            })
                            );
                        }, toolExecutorPool))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                futures.forEach(f -> {
                    ToolResult verified = f.join();
                    log.debug("Tool '{}' result [success={}]: {}", verified.toolName(), verified.success(), verified.result());
                    contextManager.addToolResult(verified.toolName(), verified.result());
                });
            }
        }

        log.warn("Max iterations ({}) reached — tokens used: {}", maxIterations, totalUsage);
        return AgentResponse.of("Max iterations reached without a final response.", toolCallTrace, totalUsage);
    }
}
