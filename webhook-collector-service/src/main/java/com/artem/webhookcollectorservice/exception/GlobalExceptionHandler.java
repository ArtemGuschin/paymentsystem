package com.artem.webhookcollectorservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidWebhookException.class)
    public ResponseEntity<Void> handleInvalidWebhook(
            InvalidWebhookException exception
    ) {
        return ResponseEntity.badRequest().build();
    }
}