package io.github.ashwith.model;

import io.github.ashwith.context.Message;
import io.github.ashwith.dto.ModelResponse;
import io.github.ashwith.tools.LLMTool;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface Model {

    ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools);

    default ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools, Consumer<String> onToken) {
        return invoke(messages, tools);
    }
}
