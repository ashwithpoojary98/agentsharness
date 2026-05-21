package io.github.ashwith.verification;

import io.github.ashwith.dto.ToolResult;

@FunctionalInterface
public interface Verifier {
    ToolResult verify(ToolResult result);
}
