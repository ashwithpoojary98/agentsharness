package io.github.ashwith.context;

import io.github.ashwith.dto.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    private final String systemPrompt;
    private final int maxMessages;
    private final Deque<Message> history;
    private final ReentrantLock lock = new ReentrantLock();

    public ContextManager(String systemPrompt, int maxMessages) {
        this.systemPrompt = systemPrompt;
        this.maxMessages = maxMessages;
        this.history = new ArrayDeque<>();
        log.debug("Initialized with maxMessages={}", maxMessages);
    }

    public void addUserMessage(String content) {
        lock.lock();
        try {
            evictIfFull();
            history.addLast(Message.user(content));
            log.debug("Added USER message — history size: {}", history.size());
        } finally {
            lock.unlock();
        }
    }

    public void addAssistantMessage(String content) {
        lock.lock();
        try {
            history.addLast(Message.assistant(content));
            log.debug("Added ASSISTANT message — history size: {}", history.size());
        } finally {
            lock.unlock();
        }
    }

    public void addAssistantToolCalls(List<ToolCall> toolCalls) {
        lock.lock();
        try {
            history.addLast(Message.assistantWithToolCalls(toolCalls));
            log.debug("Added ASSISTANT tool-call message ({} calls) — history size: {}", toolCalls.size(), history.size());
        } finally {
            lock.unlock();
        }
    }

    public void addToolResult(String toolName, String result) {
        lock.lock();
        try {
            history.addLast(Message.tool(result));
            log.debug("Added TOOL result for '{}' — history size: {}", toolName, history.size());
        } finally {
            lock.unlock();
        }
    }

    public List<Message> getMessages() {
        lock.lock();
        try {
            var messages = new ArrayList<Message>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Message.system(systemPrompt));
            }
            messages.addAll(history);
            log.debug("Built context: {} message(s) (including system prompt)", messages.size());
            return messages;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            history.clear();
            log.info("Context cleared");
        } finally {
            lock.unlock();
        }
    }

    private void evictIfFull() {
        while (history.size() >= maxMessages) {
            history.pollFirst();
            log.debug("Context full — evicted oldest message");
        }
    }
}
