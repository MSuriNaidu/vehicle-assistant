package com.vigidrive.vehicleassistant.config;

import com.vigidrive.vehicleassistant.agent.AdvisorAgent;
import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.DiagnosisAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.Urgency;
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
    public AdvisorAgent advisorAgent(ChatModel chatModel, VehicleTools vehicleTools) {
        return AgenticServices.agentBuilder(AdvisorAgent.class)
                .chatModel(chatModel)
                .tools(vehicleTools)
                .build();
    }

    @Bean
    public CoordinatorAgent coordinatorAgent(DiagnosisAgent diagnosisAgent,
                                             AdvisorAgent advisorAgent) {
        return AgenticServices.sequenceBuilder(CoordinatorAgent.class)
                .subAgents(diagnosisAgent, advisorAgent)
                .outputKey("adviceResponse")
                .output(scope -> {
                    Object diagnosisValue = scope.readState("diagnosis");
                    Diagnosis diagnosis = diagnosisValue instanceof Diagnosis value ? value : null;
                    Urgency urgency = diagnosis == null
                            ? null
                            : Urgency.fromModelValue(diagnosis.urgency());
                    return new AdviceResponse(urgency, (String) scope.readState("answer"), false, null);
                })
                .build();
    }

}
