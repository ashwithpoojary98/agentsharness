package io.github.ashwith.context;

import io.github.ashwith.dto.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    private final String systemPrompt;
    private final int maxMessages;
    private final Deque<Message> history;

    public ContextManager(String systemPrompt, int maxMessages) {
        this.systemPrompt = systemPrompt;
        this.maxMessages = maxMessages;
        this.history = new ArrayDeque<>();
        log.debug("Initialized with maxMessages={}", maxMessages);
    }

    public void addUserMessage(String content) {
        evictIfFull();
        history.addLast(Message.user(content));
        log.debug("Added USER message — history size: {}", history.size());
    }

    public void addAssistantMessage(String content) {
        history.addLast(Message.assistant(content));
        log.debug("Added ASSISTANT message — history size: {}", history.size());
    }

    public void addAssistantToolCalls(List<ToolCall> toolCalls) {
        history.addLast(Message.assistantWithToolCalls(toolCalls));
        log.debug("Added ASSISTANT tool-call message ({} calls) — history size: {}", toolCalls.size(), history.size());
    }

    public void addToolResult(String toolName, String result) {
        history.addLast(Message.tool(result));
        log.debug("Added TOOL result for '{}' — history size: {}", toolName, history.size());
    }

    public List<Message> getMessages() {
        var messages = new ArrayList<Message>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Message.system(systemPrompt));
        }
        messages.addAll(history);
        log.debug("Built context: {} message(s) (including system prompt)", messages.size());
        return messages;
    }

    public void clear() {
        history.clear();
        log.info("Context cleared");
    }

    private void evictIfFull() {
        while (history.size() >= maxMessages) {
            history.pollFirst();
            log.debug("Context full — evicted oldest message");
        }
    }
}
