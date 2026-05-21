package io.github.ashwith.guardrail;

import io.github.ashwith.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public class RateLimitGuardrail implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGuardrail.class);

    private final int maxRequests;
    private final long windowMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public RateLimitGuardrail(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
        log.debug("Configured: max {} requests per {}s", maxRequests, window.toSeconds());
    }

    @Override
    public synchronized GuardrailResult check(AgentRequest request) {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;

        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        log.debug("Rate check: {}/{} requests in window", timestamps.size(), maxRequests);

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded: {} requests in {}ms window", timestamps.size(), windowMillis);
            return new GuardrailResult.Block(
                    "Rate limit exceeded. Max " + maxRequests + " requests per " + (windowMillis / 1000) + " seconds."
            );
        }

        timestamps.addLast(now);
        return new GuardrailResult.Pass(request);
    }
}
