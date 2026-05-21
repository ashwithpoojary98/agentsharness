package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlocklistGuardrail implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(BlocklistGuardrail.class);

    private final List<String> blockedTerms;

    public BlocklistGuardrail(List<String> blockedTerms) {
        this.blockedTerms = List.copyOf(blockedTerms);
        log.debug("Configured with {} blocked term(s): {}", blockedTerms.size(), blockedTerms);
    }

    @Override
    public GuardrailResult check(AgentRequest request) {
        String lowered = request.userInput().toLowerCase();
        log.debug("Scanning input against blocklist");

        return blockedTerms.stream()
                .filter(term -> lowered.contains(term.toLowerCase()))
                .findFirst()
                .<GuardrailResult>map(term -> {
                    log.warn("Blocked term detected: '{}'", term);
                    return new GuardrailResult.Block("Input contains restricted content.");
                })
                .orElseGet(() -> {
                    log.debug("No blocked terms found");
                    return new GuardrailResult.Pass(request);
                });
    }
}
