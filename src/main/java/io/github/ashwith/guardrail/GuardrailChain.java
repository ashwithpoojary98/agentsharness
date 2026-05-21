package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import io.github.ashwith.dto.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuardrailChain {

    private static final Logger log = LoggerFactory.getLogger(GuardrailChain.class);

    private final List<Guardrail> guardrails;

    public GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = new ArrayList<>(guardrails);
    }

    public Optional<AgentResponse> process(AgentRequest request) {
        log.debug("Running {} guardrail(s)", guardrails.size());

        for (Guardrail guardrail : guardrails) {
            String name = guardrail.getClass().getSimpleName();
            log.debug("Checking guardrail: {}", name);

            switch (guardrail.check(request)) {
                case GuardrailResult.Block b -> {
                    log.warn("[{}] BLOCKED — {}", name, b.reason());
                    return Optional.of(AgentResponse.blocked(b.reason()));
                }
                case GuardrailResult.DirectResponse d -> {
                    log.info("[{}] DIRECT RESPONSE — skipping LLM", name);
                    return Optional.of(AgentResponse.directResponse(d.response()));
                }
                case GuardrailResult.Pass p -> {
                    log.debug("[{}] PASSED", name);
                    request = p.request();
                }
            }
        }

        log.debug("All guardrails passed — proceeding to model");
        return Optional.empty();
    }
}
