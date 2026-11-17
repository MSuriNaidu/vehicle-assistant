package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.service.V;

public interface CoordinatorAgent {

    AdviceResponse coordinate(@V("message") String message,
                              @V("vehicleIssue") VehicleIssue vehicleIssue,
                              @V("responseLanguage") String responseLanguage);
}
