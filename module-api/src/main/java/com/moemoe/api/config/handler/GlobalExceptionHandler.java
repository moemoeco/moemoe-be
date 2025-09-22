package com.moemoe.api.config.handler;

import com.moemoe.client.exception.ClientRuntimeException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {IllegalArgumentException.class, ClientRuntimeException.class})
    protected ResponseEntity<ErrorResponseBody> handleInternalServerError(Exception exception) {
        log.error("Handling internal server error.", exception);
        return new ResponseEntity<>(ErrorResponseBody.of(exception), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {HandlerMethodValidationException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    protected ResponseEntity<ErrorResponseBody> handleBadRequestError(Exception exception) {
        log.error("Handling bad request error.", exception);
        return new ResponseEntity<>(ErrorResponseBody.of(exception), HttpStatus.BAD_REQUEST);
    }
}
