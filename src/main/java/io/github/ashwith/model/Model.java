package io.github.ashwith.model;

import io.github.ashwith.context.Message;
import io.github.ashwith.dto.ModelResponse;
import io.github.ashwith.tools.LLMTool;

import java.util.Collection;
import java.util.List;

public interface Model {
    ModelResponse invoke(List<Message> messages, Collection<LLMTool> tools);
}
