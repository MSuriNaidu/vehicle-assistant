package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_MODEL_OUTPUT_MESSAGE =
            "I couldn't reliably assess the urgency. Please try again or consult a qualified mechanic.";
    private static final String TEMPORARY_PROVIDER_MESSAGE =
            "The assistant is temporarily unavailable. Please wait a few minutes and try again.";
    private static final String GENERAL_ERROR_MESSAGE =
            "We couldn't complete your request. Please try again.";

    @ExceptionHandler(InvalidModelOutputException.class)
    public ResponseEntity<ApiError> handleInvalidModelOutput(InvalidModelOutputException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(INVALID_MODEL_OUTPUT_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        if (isProviderCapacityError(exception)) {
            log.warn("AI provider is temporarily unavailable: {}", exception.toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "120")
                    .body(new ApiError(TEMPORARY_PROVIDER_MESSAGE));
        }

        log.error("Vehicle assistant request could not be completed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(GENERAL_ERROR_MESSAGE));
    }

    private static boolean isProviderCapacityError(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("in_flight_budget_exhausted")
                    || normalized.contains("free-models-per-day")
                    || normalized.contains("rate limit")
                    || normalized.contains("ratelimit")
                    || normalized.contains("\"code\":402")
                    || normalized.contains("\"code\": 402")
                    || normalized.contains("\"code\":429")
                    || normalized.contains("\"code\": 429")) {
                return true;
            }
        }
        return false;
    }
}
