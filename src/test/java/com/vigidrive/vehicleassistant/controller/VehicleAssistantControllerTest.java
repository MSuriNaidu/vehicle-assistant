package com.vigidrive.vehicleassistant.controller;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.AdviceUrgency;
import com.vigidrive.vehicleassistant.model.ApiError;
import com.vigidrive.vehicleassistant.model.ResponseLanguage;
import com.vigidrive.vehicleassistant.service.VehicleAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleAssistantControllerTest {

    private final VehicleAssistantService service = mock(VehicleAssistantService.class);
    private final VehicleAssistantController controller = new VehicleAssistantController(service);

    @Test
    void postsJsonQuestionToAssistant() {
        AdviceResponse expected = new AdviceResponse(
                AdviceUrgency.SERVICE_SOON, "Get it checked soon.", false, null);
        when(service.ask("My car makes a noise")).thenReturn(expected);

        ResponseEntity<?> response = controller.chat(
                new VehicleAssistantController.ChatRequest("  My car makes a noise  "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).ask("My car makes a noise");
    }

    @Test
    void rejectsBlankJsonQuestion() {
        ResponseEntity<?> response = controller.chat(new VehicleAssistantController.ChatRequest("  "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(new ApiError("Please enter a vehicle question."), response.getBody());
    }

    @Test
    void passesTheSelectedResponseLanguage() {
        AdviceResponse expected = new AdviceResponse(
                AdviceUrgency.SERVICE_SOON, "Revise pronto.", false, null);
        when(service.ask("My brake squeaks", ResponseLanguage.TELUGU)).thenReturn(expected);

        ResponseEntity<?> response = controller.chat(
                new VehicleAssistantController.ChatRequest("My brake squeaks", ResponseLanguage.TELUGU));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).ask("My brake squeaks", ResponseLanguage.TELUGU);
    }
}
