package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.ApiError;
import com.vigidrive.vehicleassistant.service.VehicleAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class VehicleAssistantController {


    private final VehicleAssistantService vehicleAssistantService;

    @GetMapping("/chat")
    public ResponseEntity<AdviceResponse> chat(@RequestParam String question) {
        return ResponseEntity.ok(vehicleAssistantService.ask(question));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("Please enter a vehicle question."));
        }
        return ResponseEntity.ok(vehicleAssistantService.ask(request.question().trim()));
    }

    public record ChatRequest(String question) {
    }

}
