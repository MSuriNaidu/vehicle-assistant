package com.vigidrive.vehicleassistant.config;

import com.vigidrive.vehicleassistant.agent.AdvisorAgent;
import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.DiagnosisAgent;
import com.vigidrive.vehicleassistant.agent.DiagnosisParserAgent;
import com.vigidrive.vehicleassistant.agent.DefaultCoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.safety.SafetyService;
import com.vigidrive.vehicleassistant.service.VehicleIssueValidator;
import com.vigidrive.vehicleassistant.tools.VehicleTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AgentConfig {

    @Bean("intakeChatModel")
    public ChatModel intakeChatModel(
            @Value("${GROQ_BASE_URL:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${GROQ_API_KEY:}") String apiKey,
            @Value("${GROQ_MODEL:openai/gpt-oss-120b}") String modelName) {
        return chatModelBuilder(baseUrl, apiKey, modelName)
                .responseFormat("json_schema")
                .build();
    }

    @Bean("assistantChatModel")
    public ChatModel assistantChatModel(
            @Value("${GROQ_BASE_URL:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${GROQ_API_KEY:}") String apiKey,
            @Value("${GROQ_MODEL:openai/gpt-oss-120b}") String modelName) {
        return chatModelBuilder(baseUrl, apiKey, modelName).build();
    }

    @Bean
    public IntakeAgent intakeAgent(@Qualifier("intakeChatModel") ChatModel chatModel) {
        return AgenticServices.agentBuilder(IntakeAgent.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public DiagnosisAgent diagnosisAgent(@Qualifier("assistantChatModel") ChatModel chatModel,
                                         VehicleTools vehicleTools) {
        return AgenticServices.agentBuilder(DiagnosisAgent.class)
                .chatModel(chatModel)
                .tools(vehicleTools)
                .build();
    }

    @Bean
    public DiagnosisParserAgent diagnosisParserAgent(@Qualifier("intakeChatModel") ChatModel chatModel) {
        return AgenticServices.agentBuilder(DiagnosisParserAgent.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public AdvisorAgent advisorAgent(@Qualifier("assistantChatModel") ChatModel chatModel) {
        return AgenticServices.agentBuilder(AdvisorAgent.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public CoordinatorAgent coordinatorAgent(IntakeAgent intakeAgent,
                                             DiagnosisAgent diagnosisAgent,
                                             DiagnosisParserAgent diagnosisParserAgent,
                                             AdvisorAgent advisorAgent,
                                             SafetyService safetyService,
                                             VehicleIssueValidator issueValidator,
                                             VehicleTools vehicleTools) {
        return new DefaultCoordinatorAgent(intakeAgent, diagnosisAgent, diagnosisParserAgent,
                advisorAgent, safetyService, issueValidator, vehicleTools);
    }

    private static OpenAiChatModel.OpenAiChatModelBuilder chatModelBuilder(
            String baseUrl, String apiKey, String modelName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set GROQ_API_KEY before starting the vehicle assistant.");
        }
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .maxRetries(1)
                .maxTokens(3000)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false);
    }

}
