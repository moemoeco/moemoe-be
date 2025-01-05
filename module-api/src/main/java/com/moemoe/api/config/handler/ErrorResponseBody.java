package com.moemoe.api.config.handler;

import lombok.Getter;

@Getter
public class ErrorResponseBody {
    private final String type;
    private final String message;

    private ErrorResponseBody(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ErrorResponseBody of(Exception ex) {
        return new ErrorResponseBody(ex.getClass().getSimpleName(), ex.getMessage());
    }
}