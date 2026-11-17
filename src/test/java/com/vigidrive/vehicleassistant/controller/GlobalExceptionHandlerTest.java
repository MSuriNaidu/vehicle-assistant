package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void returnsBadGatewayWithSafeRetryGuidanceForInvalidModelOutput() {
        var response = handler.handleInvalidModelOutput(
                new InvalidModelOutputException("Unsupported diagnosis urgency 'MEDIUM'."));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(
                new ApiError("I couldn't reliably assess the urgency. Please try again or consult a qualified mechanic."),
                response.getBody());
    }

    @Test
    void returnsRetryGuidanceInsteadOfProviderDetailsWhenCapacityIsExhausted() {
        var exception = new RuntimeException("Agent invocation failed",
                new RuntimeException("{\"error\":{\"code\":402,\"metadata\":{\"reason\":\"in_flight_budget_exhausted\"}}}"));

        var response = handler.handleUnexpectedException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("120", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(
                new ApiError("The assistant is temporarily unavailable. Please wait a few minutes and try again."),
                response.getBody());
    }

    @Test
    void returnsGenericMessageForUnexpectedErrorsWithoutExposingExceptionDetails() {
        var response = handler.handleUnexpectedException(
                new RuntimeException("VehicleIssue.java failed at line 42"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(new ApiError("We couldn't complete your request. Please try again."),
                response.getBody());
    }
}
