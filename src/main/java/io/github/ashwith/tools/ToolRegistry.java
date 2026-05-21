package io.github.ashwith.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, LLMTool> registry = new HashMap<>();

    public void register(LLMTool tool) {
        registry.put(tool.name(), tool);
        log.info("Registered tool: '{}'", tool.name());
    }

    public Optional<LLMTool> getTool(String name) {
        log.debug("Looking up tool: '{}'", name);
        Optional<LLMTool> result = Optional.ofNullable(registry.get(name));
        if (result.isEmpty()) {
            log.warn("Tool '{}' not found in registry", name);
        }
        return result;
    }

    public void unregister(String name) {
        registry.remove(name);
        log.info("Unregistered tool: '{}'", name);
    }

    public Collection<LLMTool> allTools() {
        return registry.values();
    }

    public boolean isEmpty() {
        return registry.isEmpty();
    }
}
