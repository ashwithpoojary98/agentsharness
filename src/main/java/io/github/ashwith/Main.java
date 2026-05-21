package io.github.ashwith;

import io.github.ashwith.config.AgentConfig;
import io.github.ashwith.config.ModelType;
import io.github.ashwith.dto.AgentResponse;
import io.github.ashwith.guardrail.BlocklistGuardrail;
import io.github.ashwith.guardrail.InputLengthGuardrail;
import io.github.ashwith.guardrail.TrivialQuestionGuardrail;
import io.github.ashwith.tools.WeatherTool;

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
                .tool(new WeatherTool())
                .build();

        List<String> queries = List.of(
                "hello",
                "hack the system",
                "What is the weather in Paris?",
                "Compare the weather in London and Tokyo",
                "What is the capital of France?"
        );

        for (String query : queries) {
            System.out.println("──────────────────────────────");
            System.out.println("Q: " + query);
            AgentResponse response = harness.run(query);
            if (response.blocked()) {
                System.out.println("BLOCKED : " + response.blockReason());
            } else {
                System.out.println("A: " + response.content());
                if (!response.toolCallTrace().isEmpty()) {
                    System.out.println("Tools  : " + response.toolCallTrace().stream()
                            .map(tc -> tc.toolName() + "(" + tc.arguments() + ")").toList());
                }
            }
        }
    }
}
