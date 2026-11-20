package com.vigidrive.vehicleassistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import dev.langchain4j.model.chat.ChatModel;
import com.vigidrive.vehicleassistant.agent.DiagnosisParserAgent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "GROQ_API_KEY=test-api-key")
class VehicleAssistantApplicationTests {

	@Autowired
	private Environment environment;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DiagnosisParserAgent diagnosisParserAgent;

	@Test
	void contextLoads() {
		assertEquals(
				"test-api-key",
				environment.getProperty("GROQ_API_KEY"));
		assertNotNull(applicationContext.getBean("intakeChatModel", ChatModel.class));
		assertNotNull(applicationContext.getBean("assistantChatModel", ChatModel.class));
		org.junit.jupiter.api.Assertions.assertNotNull(diagnosisParserAgent);
	}

}
