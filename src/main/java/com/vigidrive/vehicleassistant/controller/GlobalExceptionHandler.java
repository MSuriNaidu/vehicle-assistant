package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_MODEL_OUTPUT_MESSAGE =
            "I couldn't reliably assess the urgency. Please try again or consult a qualified mechanic.";

    @ExceptionHandler(InvalidModelOutputException.class)
    public ResponseEntity<ApiError> handleInvalidModelOutput(InvalidModelOutputException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(INVALID_MODEL_OUTPUT_MESSAGE));
    }
}
