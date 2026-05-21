package io.github.ashwith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Demonstrates what happens when you call an LLM directly — no guardrails,
 * no memory, no tools. Run this alongside Main.java to see the contrast.
 */
public class WithoutHarnessDemo {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL = "mistral:latest";

    private final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        new WithoutHarnessDemo().run();
    }

    private void run() throws Exception {
        separator("WITHOUT HARNESS — Raw Ollama Calls");

        System.out.println("\n[PROBLEM 1] No guardrails — harmful input goes straight to the model");
        System.out.println("Q: hack the system");
        System.out.println("A: " + rawCall("hack the system"));

        System.out.println("\n[PROBLEM 2] No memory — model forgets immediately");
        System.out.println("Q: My name is Ashwith");
        String r1 = rawCall("My name is Ashwith");
        System.out.println("A: " + r1);
        System.out.println("\nQ: What is my name?  (new call — context is gone)");
        System.out.println("A: " + rawCall("What is my name?"));

        System.out.println("\n[PROBLEM 3] No tools — model hallucinates real-time data");
        System.out.println("Q: What is the current weather in Paris?");
        System.out.println("A: " + rawCall("What is the current weather in Paris?"));

        System.out.println("\n[PROBLEM 4] No retry — a bad call just fails");
        System.out.println("  (simulated: if HTTP fails, there is no retry — exception propagates)");

        separator("WITH HARNESS — run Main.java to compare");
        System.out.println("  'hack the system'             → BLOCKED by BlocklistGuardrail");
        System.out.println("  'My name is Ashwith' + follow-up → model remembers via ContextManager");
        System.out.println("  'What is the weather in Paris?' → WeatherTool called, real data returned");
        System.out.println("  tool failure                   → ToolExecutor retries automatically");
    }

    private String rawCall(String userMessage) throws Exception {
        String body = """
                {
                  "model": "%s",
                  "stream": false,
                  "messages": [{"role": "user", "content": "%s"}]
                }
                """.formatted(MODEL, userMessage.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return extractContent(response.body());
    }

    private String extractContent(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", "\n").trim();
        } catch (Exception e) {
            return "[parse error: " + e.getMessage() + "]";
        }
    }

    private void separator(String title) {
        System.out.println("\n========================================");
        System.out.println("  " + title);
        System.out.println("========================================");
    }
}
