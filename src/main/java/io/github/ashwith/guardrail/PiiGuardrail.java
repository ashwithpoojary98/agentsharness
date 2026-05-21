package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiGuardrail implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(PiiGuardrail.class);

    public enum Action { BLOCK, MASK }

    private record PiiPattern(String label, Pattern regex, String mask) {}

    private static final List<PiiPattern> PATTERNS = List.of(
            new PiiPattern("email",
                    Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
                    "[EMAIL]"),
            new PiiPattern("phone",
                    Pattern.compile("\\b(\\+\\d{1,3}[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4})\\b"),
                    "[PHONE]"),
            new PiiPattern("credit-card",
                    Pattern.compile("\\b(?:\\d[ \\-]*){13,16}\\b"),
                    "[CARD]"),
            new PiiPattern("ssn",
                    Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
                    "[SSN]")
    );

    private final Action action;

    public PiiGuardrail(Action action) {
        this.action = action;
        log.debug("Configured with action={}", action);
    }

    @Override
    public GuardrailResult check(AgentRequest request) {
        String input = request.userInput();

        for (PiiPattern pii : PATTERNS) {
            Matcher matcher = pii.regex().matcher(input);
            if (matcher.find()) {
                log.warn("PII detected: type={}", pii.label());

                if (action == Action.BLOCK) {
                    return new GuardrailResult.Block(
                            "Input contains personal information (" + pii.label() + ") that cannot be processed."
                    );
                }

                String masked = matcher.replaceAll(pii.mask());
                log.info("PII masked: type={} → passing sanitised input", pii.label());
                return new GuardrailResult.Pass(new AgentRequest(masked));
            }
        }

        log.debug("No PII detected");
        return new GuardrailResult.Pass(request);
    }
}
