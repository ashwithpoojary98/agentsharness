package io.github.ashwith;

import io.github.ashwith.config.AgentConfig;
import io.github.ashwith.context.ContextManager;
import io.github.ashwith.dto.AgentRequest;
import io.github.ashwith.dto.AgentResponse;
import io.github.ashwith.guardrail.Guardrail;
import io.github.ashwith.guardrail.GuardrailChain;
import io.github.ashwith.model.Model;
import io.github.ashwith.model.ModelFactory;
import io.github.ashwith.tools.LLMTool;
import io.github.ashwith.tools.ToolExecutor;
import io.github.ashwith.tools.ToolRegistry;
import io.github.ashwith.tools.ToolSelector;
import io.github.ashwith.verification.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AgentHarness {

    private static final Logger log = LoggerFactory.getLogger(AgentHarness.class);

    private final AgentLoop agentLoop;

    private AgentHarness(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    public AgentResponse run(String userInput) {
        return agentLoop.run(new AgentRequest(userInput));
    }

    public AgentResponse run(String userInput, Consumer<String> onToken) {
        return agentLoop.run(new AgentRequest(userInput), onToken);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AgentConfig config = AgentConfig.builder().build();
        private final List<Guardrail> guardrails = new ArrayList<>();
        private final ToolRegistry toolRegistry = new ToolRegistry();
        private Verifier verifier = result -> result;
        private int toolRetries = 2;

        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public Builder guardrail(Guardrail guardrail) {
            this.guardrails.add(guardrail);
            return this;
        }

        public Builder tool(LLMTool tool) {
            this.toolRegistry.register(tool);
            return this;
        }

        public Builder verifier(Verifier verifier) {
            this.verifier = verifier;
            return this;
        }

        public Builder toolRetries(int retries) {
            this.toolRetries = retries;
            return this;
        }

        public AgentHarness build() {
            log.info("Building AgentHarness — model={} ({}), guardrails={}, tools={}",
                    config.getModelName(),
                    config.getModelType(),
                    guardrails.size(),
                    toolRegistry.allTools().stream().map(LLMTool::name).toList());

            Model model = ModelFactory.create(config);
            ContextManager contextManager = new ContextManager(config.getSystemPrompt(), config.getMaxContextMessages());
            GuardrailChain guardrailChain = new GuardrailChain(guardrails);
            ToolSelector toolSelector = new ToolSelector(toolRegistry);
            ToolExecutor toolExecutor = new ToolExecutor(toolRetries);

            AgentLoop loop = new AgentLoop(
                    model, contextManager, guardrailChain,
                    toolRegistry, toolSelector, toolExecutor,
                    verifier, config.getMaxIterations()
            );

            log.info("AgentHarness ready");
            return new AgentHarness(loop);
        }
    }
}
