package com.time_tracker.be.common;

import lombok.Getter;

@Getter
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public static TokenType fromValue(String value) {
        for (TokenType type : TokenType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown token type: " + value);
    }
}
