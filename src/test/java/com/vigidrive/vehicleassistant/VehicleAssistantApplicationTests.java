package com.vigidrive.vehicleassistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "langchain4j.open-ai.chat-model.api-key=test-api-key")
class VehicleAssistantApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
		assertEquals(
				"test-api-key",
				environment.getProperty("langchain4j.open-ai.chat-model.api-key"));
	}

}
