package com.vigidrive.vehicleassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "langchain4j.open-ai.chat-model.api-key=test-api-key")
class VehicleAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
