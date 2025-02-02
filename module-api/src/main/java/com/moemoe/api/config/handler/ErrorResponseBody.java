package com.moemoe.api.config.handler;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ErrorResponseBody {
    private String type;
    private String message;

    private ErrorResponseBody(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ErrorResponseBody of(Exception ex) {
        return new ErrorResponseBody(ex.getClass().getSimpleName(), ex.getMessage());
    }

    public static ErrorResponseBody of(String type, String message) {
        return new ErrorResponseBody(type, message);
    }
}