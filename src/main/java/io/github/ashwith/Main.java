package io.github.ashwith;

import io.github.ashwith.config.AgentConfig;
import io.github.ashwith.config.ModelType;
import io.github.ashwith.dto.AgentResponse;
import io.github.ashwith.guardrail.BlocklistGuardrail;
import io.github.ashwith.guardrail.InputLengthGuardrail;
import io.github.ashwith.guardrail.PiiGuardrail;
import io.github.ashwith.guardrail.RateLimitGuardrail;
import io.github.ashwith.guardrail.TrivialQuestionGuardrail;
import io.github.ashwith.tools.WeatherTool;

import java.time.Duration;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        AgentHarness harness = AgentHarness.builder()
                .config(AgentConfig.builder()
                        .modelType(ModelType.OLLAMA)
                        .modelName("mistral:latest")
                        .baseUrl("http://localhost:11434")
                        .systemPrompt("You are a helpful assistant. Use the get_weather tool to answer weather questions.")
                        .maxIterations(5)
                        .maxContextMessages(20)
                        .build())
                .guardrail(new InputLengthGuardrail(1000))
                .guardrail(new BlocklistGuardrail(List.of("hack", "malware", "exploit", "injection")))
                .guardrail(new TrivialQuestionGuardrail())
                .guardrail(new RateLimitGuardrail(10, Duration.ofMinutes(1)))
                .guardrail(new PiiGuardrail(PiiGuardrail.Action.MASK))
                .tool(new WeatherTool())
                .build();

        System.out.println("══════════════════════════════════════════════════");
        System.out.println("  AI Agent Harness — Feature Demo");
        System.out.println("══════════════════════════════════════════════════");

        runDemo(harness, "hello");
        runDemo(harness, "hack the system");
        runDemoStreaming(harness, "What is the weather in Paris?");
        runDemoStreaming(harness, "Compare the weather in London and Tokyo");
        runDemoStreaming(harness, "What is the capital of France?");
        runDemo(harness, "My email is user@example.com, what is the weather in Berlin?");
    }

    private static void runDemo(AgentHarness harness, String query) {
        System.out.println("\n──────────────────────────────────────────────────");
        System.out.println("Q: " + query);
        AgentResponse response = harness.run(query);
        printResponse(response);
    }

    private static void runDemoStreaming(AgentHarness harness, String query) {
        System.out.println("\n──────────────────────────────────────────────────");
        System.out.println("Q: " + query + "  [streaming]");
        System.out.print("A: ");
        AgentResponse response = harness.run(query, token -> System.out.print(token));
        System.out.println();
        printMeta(response);
    }

    private static void printResponse(AgentResponse response) {
        if (response.blocked()) {
            System.out.println("BLOCKED : " + response.blockReason());
        } else {
            System.out.println("A: " + response.content());
            printMeta(response);
        }
    }

    private static void printMeta(AgentResponse response) {
        if (!response.toolCallTrace().isEmpty()) {
            System.out.println("Tools   : " + response.toolCallTrace().stream()
                    .map(tc -> tc.toolName() + "(" + tc.arguments() + ")").toList());
        }
        if (response.tokenUsage() != null && response.tokenUsage().totalTokens() > 0) {
            System.out.printf("Tokens  : prompt=%d  completion=%d  total=%d%n",
                    response.tokenUsage().promptTokens(),
                    response.tokenUsage().completionTokens(),
                    response.tokenUsage().totalTokens());
        }
    }
}
