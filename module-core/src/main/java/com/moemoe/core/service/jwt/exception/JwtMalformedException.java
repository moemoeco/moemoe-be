package com.moemoe.core.service.jwt.exception;

public class JwtMalformedException extends RuntimeException{
    public JwtMalformedException(String message, Throwable cause) {
        super(message, cause);
    }
}
