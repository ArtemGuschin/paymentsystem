package com.artem.webhookcollectorservice.exception;

public class InvalidWebhookException extends RuntimeException {

    public InvalidWebhookException(String message) {
        super(message);
    }
}
