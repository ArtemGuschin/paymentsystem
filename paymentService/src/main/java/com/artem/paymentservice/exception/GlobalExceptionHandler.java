package com.artem.paymentservice.exception;

import com.artem.paymentservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentMethodNotFound(
            PaymentMethodNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PaymentProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProviderUnavailable(
            PaymentProviderUnavailableException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError ->
                        fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex
    ) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
    }
}