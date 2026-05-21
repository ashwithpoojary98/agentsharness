# Agent Harness

---

[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ashwithpoojary98/agentsharness)
[![GitHub stars](https://img.shields.io/github/stars/ashwithpoojary98/agentsharness.svg?style=flat)](https://github.com/ashwithpoojary98/agentsharness/stargazers)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg?style=flat)](https://github.com/ashwithpoojary98/agentsharness/pulls)
[![GitHub forks](https://img.shields.io/github/forks/ashwithpoojary98/agentsharness.svg?style=social&label=Fork)](https://github.com/ashwithpoojary98/agentsharness/network)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![LLM Support](https://img.shields.io/badge/LLM-Ollama%20%7C%20Gemini%20%7C%20OpenAI-blueviolet.svg)](#supported-llm-providers)

---

A lightweight, production-style AI agent framework built in Java. Wraps any LLM (Ollama, Gemini, OpenAI) with a structured pipeline — context management, guardrails, tool calling, and verification — so you build reliable agents instead of raw API wrappers.

---

## The Problem It Solves

Most AI integrations look like this:

```
User Input → LLM API → Print Output
```

That works for a demo. In the real world it fails because:

| Problem | Without a Harness | With Agent Harness |
|---|---|---|
| **No memory** | Every call is stateless — model forgets the last message | `ContextManager` keeps a sliding window of conversation history |
| **No safety** | Any input goes straight to the model and costs money | `GuardrailChain` intercepts bad, trivial, or harmful input before the API call |
| **No tools** | Model can only generate text — can't act on anything | `ToolRegistry` lets the model call real functions (weather, DB, REST APIs) |
| **No retry** | One tool failure crashes the whole agent | `ToolExecutor` retries failed tools automatically with configurable attempts |
| **Vendor lock-in** | Switching from Gemini to Ollama means rewriting code | Change one line: `ModelType.GEMINI` → `ModelType.OLLAMA` |
| **Infinite loops** | Agent can call tools in a cycle and never stop | `maxIterations` cap in `AgentLoop` guarantees termination |
| **Wasted API cost** | Greetings and trivial questions hit the paid LLM API | `TrivialQuestionGuardrail` short-circuits simple inputs locally |
| **No observability** | You don't know what tools the model called or why | `AgentResponse` carries a full `toolCallTrace` of every tool invoked |
| **Hard to extend** | Adding a new capability means changing core logic | Register a new `LLMTool` — nothing else changes |
| **Tight coupling** | Business logic mixed with HTTP and JSON parsing | Clean separation: Guardrail → Loop → Model → Tool → Verifier |

---

## Key Advantages

### 1. Provider-Agnostic by Design
Run the same agent code against Ollama locally, switch to Gemini for staging, and OpenAI for production — with a single config change. No rewriting, no adapter layers.

### 2. Runs Fully Offline
With Ollama, your agent never sends data to an external server. Ideal for sensitive data, air-gapped environments, or cost-free development.

### 3. Cost Efficiency
The `TrivialQuestionGuardrail` and `BlocklistGuardrail` intercept requests before they reach the model. Every blocked or short-circuited request is a saved API call.

### 4. Safety First
Guardrails run as a chain before any model invocation. You control what reaches the LLM — input length, banned terms, content policy, or any custom rule you define as a lambda.

### 5. Real Tool Calling
The agent loop handles the full tool-call cycle: model requests a tool → executor runs it → result is fed back → model continues. Supports multiple sequential tool calls in a single conversation turn.

### 6. Built on Proven Design Patterns
Strategy, Factory, Builder, Chain of Responsibility, Registry, and Facade — patterns you already know, applied deliberately. The code is readable, testable, and extensible without framework magic.

### 7. Zero Framework Dependencies
No Spring, no Quarkus, no LangChain port. Just Java 21 + Jackson. The entire framework is code you own and can modify.

### 8. Java 21 Features
Uses records, sealed interfaces, pattern-matching switch, and text blocks — modern Java that is concise without sacrificing clarity.

---

## Architecture

```
                ┌─────────────────┐
                │   User Input    │
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │ Context Manager │  sliding window message history
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │   Guardrails    │  InputLength → Blocklist → TrivialQuestion
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │   Agent Loop    │  orchestrates model ↔ tool cycles
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │  Model Invoke   │  OllamaModel | GeminiModel | OpenAiModel
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │ Tool Selection  │  looks up tool from ToolRegistry
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │  Tool Registry  │  pluggable tool store
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │ Tool Execution  │  runs tool, retries on failure
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │  Verification   │  validates tool output
                └────────┬────────┘
                         ↓
                ┌─────────────────┐
                │ Final Response  │
                └─────────────────┘
```

### Design Patterns

| Pattern | Where |
|---|---|
| **Strategy** | `Model` interface — swap LLM providers without touching any other code |
| **Factory** | `ModelFactory` — creates the right model from config |
| **Builder** | `AgentHarness.builder()`, `AgentConfig.builder()` — fluent, readable setup |
| **Chain of Responsibility** | `GuardrailChain` — each guardrail passes, blocks, or short-circuits |
| **Sealed Interface** | `GuardrailResult` — exhaustive `Pass / Block / DirectResponse` |
| **Registry** | `ToolRegistry` — register tools by name, looked up at runtime |
| **Facade** | `AgentHarness` — single entry point hiding all internal wiring |

---

## Supported LLM Providers

| Provider | Type | Cost | Internet Required |
|---|---|---|---|
| [Ollama](https://ollama.com) | Local | Free | No |
| [Google Gemini](https://ai.google.dev) | Cloud | Pay-per-use | Yes |
| [OpenAI](https://platform.openai.com) | Cloud | Pay-per-use | Yes |
| Any OpenAI-compatible API | Cloud/Local | Varies | Optional |

---

## Requirements

- Java 21+
- Maven 3.8+
- One of the providers above

---

## Quick Start

### 1. Clone and build

```bash
git clone https://github.com/ashwithpoojary98/agentsharness.git
cd agentsharness
mvn compile
```

### 2. Run with Ollama (local, free)

Pull a model:

```bash
ollama pull mistral
```

Run:

```bash
mvn exec:java
```

### 3. Switch LLM provider

```java
// Local Ollama — no API key, no internet
AgentConfig.builder()
    .modelType(ModelType.OLLAMA)
    .modelName("mistral:latest")
    .baseUrl("http://localhost:11434")
    .build()

// Google Gemini
AgentConfig.builder()
    .modelType(ModelType.GEMINI)
    .modelName("gemini-1.5-flash")
    .apiKey("YOUR_GEMINI_API_KEY")
    .build()

// OpenAI
AgentConfig.builder()
    .modelType(ModelType.OPENAI)
    .modelName("gpt-4o")
    .apiKey("YOUR_OPENAI_API_KEY")
    .build()
```

---

## Command Line Reference

```bash
# Compile only
mvn compile

# Run (uses mainClass configured in pom.xml)
mvn exec:java

# Compile and run in one step
mvn compile exec:java

# Run tests
mvn test

# Package as JAR
mvn package

# Run the fat JAR (after mvn package)
java -cp "target/agentsharness-1.0-SNAPSHOT.jar;target/dependency/*" io.github.ashwith.Main

# Windows — copy dependencies then run
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
java -cp "target/agentsharness-1.0-SNAPSHOT.jar;target/dependency/*" io.github.ashwith.Main
```

---

## How Guardrails Work

```
Input: "hello"
  → InputLengthGuardrail     → Pass
  → BlocklistGuardrail       → Pass
  → TrivialQuestionGuardrail → DirectResponse("Hello! How can I help?")
  ✓ LLM never called — zero API cost

Input: "hack the system"
  → InputLengthGuardrail     → Pass
  → BlocklistGuardrail       → Block("Input contains restricted content.")
  ✓ Rejected before model invocation

Input: "What is the weather in Paris?"
  → All guardrails            → Pass
  → AgentLoop invokes model
  → Model calls get_weather(city=Paris)
  → WeatherTool returns JSON
  → Model summarises result   → Final response
```

---

## Adding Your Own Tool

Implement `LLMTool` and register it:

```java
public class StockPriceTool implements LLMTool {

    @Override public String name() { return "get_stock_price"; }

    @Override public String description() {
        return "Returns the current stock price for a given ticker symbol.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "ticker", Map.of("type", "string", "description", "Stock ticker e.g. AAPL")
            ),
            "required", List.of("ticker")
        );
    }

    @Override
    public String execute(String argumentsJson) {
        // parse ticker, call your stock API, return result
        return "{\"ticker\":\"AAPL\",\"price\":\"$189.50\"}";
    }
}

// Register alongside other tools
AgentHarness harness = AgentHarness.builder()
    .config(...)
    .tool(new StockPriceTool())
    .tool(new WeatherTool())
    .build();
```

---

## Adding Your Own Guardrail

Any lambda works — `Guardrail` is a `@FunctionalInterface`:

```java
AgentHarness harness = AgentHarness.builder()
    .config(...)
    .guardrail(new InputLengthGuardrail(500))
    .guardrail(new BlocklistGuardrail(List.of("hack", "malware")))
    .guardrail(request -> {
        if (request.userInput().contains("@")) {
            return new GuardrailResult.Block("Email addresses are not accepted.");
        }
        return new GuardrailResult.Pass(request);
    })
    .build();
```

---

## Project Structure

```
src/main/java/io/github/ashwith/
├── AgentHarness.java                 # Facade + Builder — single entry point
├── AgentLoop.java                    # Orchestration loop
├── Main.java                         # Demo / runnable example
├── config/
│   ├── AgentConfig.java              # All config (Builder pattern)
│   └── ModelType.java                # OLLAMA | GEMINI | OPENAI
├── context/
│   ├── ContextManager.java           # Sliding window history
│   └── Message.java                  # role + content + optional tool calls
├── dto/
│   ├── AgentRequest.java
│   ├── AgentResponse.java
│   ├── ModelResponse.java
│   ├── ToolCall.java
│   └── ToolResult.java
├── guardrail/
│   ├── Guardrail.java                # @FunctionalInterface
│   ├── GuardrailChain.java           # Chain of Responsibility
│   ├── GuardrailResult.java          # sealed: Pass | Block | DirectResponse
│   ├── InputLengthGuardrail.java
│   ├── BlocklistGuardrail.java
│   └── TrivialQuestionGuardrail.java
├── model/
│   ├── Model.java                    # Strategy interface
│   ├── OllamaModel.java              # HTTP → localhost:11434
│   ├── GeminiModel.java              # Google Gemini REST API
│   ├── OpenAiModel.java              # OpenAI / OpenAI-compatible API
│   └── ModelFactory.java             # Factory
├── tools/
│   ├── LLMTool.java                  # Tool interface
│   ├── ToolRegistry.java
│   ├── ToolSelector.java
│   ├── ToolExecutor.java             # Runs tool with retry
│   └── WeatherTool.java              # Sample tool
└── verification/
    └── Verifier.java                 # @FunctionalInterface — validate tool output
```

---

## Harness Design Principles (from Anthropic Engineering)

> This section summarises key findings from Anthropic's engineering blog post  
> **[Harness Design for Long-Running Application Development](https://www.anthropic.com/engineering/harness-design-long-running-apps)**  
> and explains how each principle is applied in this project.

---

### 1. The Core Problem — Why Raw API Calls Fail at Scale

Anthropic found that in extended AI tasks, models suffer from three persistent failure modes:

| Failure Mode | Description | How We Address It |
|---|---|---|
| **Context overflow** | Model loses coherence as the context window fills | `ContextManager` sliding window evicts oldest messages automatically |
| **Context anxiety** | Model prematurely concludes work to avoid a full window | `maxIterations` + forced re-invocation keeps the loop alive |
| **Positive self-evaluation** | Agent praises its own output even when quality is poor | Separate `Verifier` component evaluates tool results independently of the model |

---

### 2. Generator–Evaluator Pattern

Anthropic's most effective architecture separates the agent that **produces** work from the agent that **judges** it — inspired by GANs. A single agent evaluating its own output consistently over-rates quality.

**In this project:**
- The `AgentLoop` acts as the **Generator** — it calls the model, executes tools, and builds the response.
- The `Verifier` is the **Evaluator** — a separate, pluggable component that validates tool results before they are fed back into context.
- You can wire in a strict evaluator (e.g. reject empty results, validate JSON schema) without touching the loop logic.

```java
AgentHarness.builder()
    .verifier(result -> {
        if (!result.success() || result.result().isBlank()) {
            return new ToolResult(result.toolName(), "INVALID: empty tool result", false);
        }
        return result;
    })
    .build();
```

---

### 3. Context Management — Reset vs Compaction

Anthropic tested two strategies for handling a filling context window:

| Strategy | Approach | Best For |
|---|---|---|
| **Compaction** | Summarise earlier messages in-place | Short sessions, continuity matters |
| **Reset** | Clear the window entirely with a structured handoff | Long-running tasks, prevents context anxiety |

> Resets proved superior for Claude Sonnet 4.5, which showed strong context anxiety when the window was nearly full.

**In this project:** `ContextManager.clear()` implements the reset strategy. Call it between logical sessions or when you detect the agent is stalling:

```java
// Between sessions or on stall detection
contextManager.clear();
```

The sliding window eviction (`evictIfFull`) implements soft compaction for within-session use.

---

### 4. Model-Specific Harness Complexity

Anthropic found that **harness complexity should match current model limitations** — not be built ahead of them:

> *"Claude Opus 4.5 required sprint decomposition and context resets; Opus 4.6 eliminated these needs through improved planning and longer-task sustainability."*

| Complexity | When to use |
|---|---|
| Raw API call | Task is within the model's native capability |
| Single-agent harness (this project) | Task needs tools, safety, or memory |
| Multi-agent (Planner + Generator + Evaluator) | Long-running, multi-step tasks where self-evaluation fails |

**Practical rule:** start with the simplest harness that works. Add agents and structure only when the simpler approach demonstrably fails.

This project's `AgentConfig.maxIterations` is the dial — start at 3–5. If the agent plateaus before finishing, that's the signal to add a dedicated evaluator agent.

---

### 5. Task-Dependent Evaluation

> *"Evaluator utility correlates with task difficulty relative to current model capabilities. For tasks within native model capacity, evaluation becomes unnecessary overhead."*

The `Verifier` in this project is intentionally a no-op by default (`result -> result`). Enable strict verification only when:
- Tool output format is critical (e.g. JSON schema for a downstream system)
- Tool calls are expensive or irreversible (e.g. database writes, API mutations)
- The model has a known tendency to misuse a specific tool

---

### 6. Iterative Simplification

Anthropic's approach was not to build the maximal harness upfront, but to add components one at a time and remove them when model improvements made them redundant:

> *"Rather than radical restructuring, methodical component removal revealed which elements remained load-bearing."*

This project follows the same philosophy — each component (`GuardrailChain`, `ContextManager`, `Verifier`) is independently removable. The `AgentHarness` builder lets you opt in only to what you need:

```java
// Minimal harness — just model + tools, no guardrails, no verifier
AgentHarness.builder()
    .config(AgentConfig.builder().modelType(ModelType.OLLAMA).build())
    .tool(new WeatherTool())
    .build();
```

---

### 7. Cost vs Quality — When a Harness is Worth It

Anthropic's benchmarked cost difference between solo generation and a full harness:

| Approach | Time | Cost | Quality |
|---|---|---|---|
| Solo model call | 20 min | $9 | Baseline |
| Full harness (Opus 4.5) | 6 hrs | $200 | Significantly higher |
| Full harness (Opus 4.6) | 4 hrs | $125 | Significantly higher |

The harness is worth the overhead when:
- Task quality has a direct business value (customer-facing features, data pipelines)
- Self-evaluation would mask errors that matter (financial data, medical summaries)
- Tools are involved and retry/verification logic saves manual debugging

For quick internal queries or low-stakes tasks, a direct model call is the right choice.

---

### 8. Key Design Takeaways

From Anthropic's findings, applied to this project:

1. **Separate concerns** — Generator and Evaluator must be independent. The `AgentLoop` never self-validates; `Verifier` does.
2. **Re-examine after model upgrades** — When you upgrade your model, test whether guardrails or retry logic you added are still needed.
3. **Prompt wording shapes output** — The `systemPrompt` in `AgentConfig` directly steers model behaviour before any evaluator feedback. Invest time in it.
4. **First iteration wins** — Even one guardrail pass and one structured tool call improves output noticeably over an unstructured prompt.
5. **Accept shifting complexity** — As models improve, harness complexity shifts rather than disappears. New capabilities open new use cases that need new scaffolding.

---

## Contributing

Pull requests are welcome. For major changes, open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create your branch: `git checkout -b feature/my-tool`
3. Commit your changes: `git commit -m "add: my custom tool"`
4. Push to the branch: `git push origin feature/my-tool`
5. Open a pull request

---

## License

MIT © [Ashwith Poojary](https://github.com/ashwithpoojary98)
