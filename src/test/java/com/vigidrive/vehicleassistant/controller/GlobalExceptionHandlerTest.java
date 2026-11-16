package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void returnsBadGatewayWithSafeRetryGuidanceForInvalidModelOutput() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleInvalidModelOutput(
                new InvalidModelOutputException("Unsupported diagnosis urgency 'MEDIUM'."));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(
                new ApiError("I couldn't reliably assess the urgency. Please try again or consult a qualified mechanic."),
                response.getBody());
    }
}
