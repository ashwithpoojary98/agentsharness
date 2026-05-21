package io.github.ashwith.dto;

public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static TokenUsage zero() {
        return new TokenUsage(0, 0, 0);
    }

    public TokenUsage add(TokenUsage other) {
        return new TokenUsage(
                this.promptTokens + other.promptTokens,
                this.completionTokens + other.completionTokens,
                this.totalTokens + other.totalTokens
        );
    }

    @Override
    public String toString() {
        return "prompt=" + promptTokens + " completion=" + completionTokens + " total=" + totalTokens;
    }
}
