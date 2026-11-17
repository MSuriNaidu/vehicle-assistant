package com.vigidrive.vehicleassistant.config;

import com.vigidrive.vehicleassistant.agent.AdvisorAgent;
import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.DiagnosisAgent;
import com.vigidrive.vehicleassistant.agent.DefaultCoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.tools.VehicleTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {


    @Bean
    public IntakeAgent intakeAgent(ChatModel chatModel, VehicleTools vehicleTools) {
        return AgenticServices.agentBuilder(IntakeAgent.class)
                .chatModel(chatModel)
                .tools(vehicleTools)
                .build();
    }

    @Bean
    public DiagnosisAgent diagnosisAgent(ChatModel chatModel, VehicleTools vehicleTools) {
        return AgenticServices.agentBuilder(DiagnosisAgent.class)
                .chatModel(chatModel)
                .tools(vehicleTools)
                .build();
    }

    @Bean
    public AdvisorAgent advisorAgent(ChatModel chatModel) {
        return AgenticServices.agentBuilder(AdvisorAgent.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public CoordinatorAgent coordinatorAgent(DiagnosisAgent diagnosisAgent,
                                             AdvisorAgent advisorAgent,
                                             VehicleTools vehicleTools) {
        return new DefaultCoordinatorAgent(diagnosisAgent, advisorAgent, vehicleTools);
    }

}
