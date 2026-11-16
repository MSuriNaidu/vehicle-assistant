package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.service.VehicleAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

}
