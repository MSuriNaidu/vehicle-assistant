package com.vigidrive.vehicleassistant.tools;

import com.vigidrive.vehicleassistant.knowledge.DtcCode;
import com.vigidrive.vehicleassistant.knowledge.ModelInfo;
import com.vigidrive.vehicleassistant.knowledge.RepairCost;
import com.vigidrive.vehicleassistant.knowledge.repository.DtcCodeRepository;
import com.vigidrive.vehicleassistant.knowledge.repository.ModelInfoRepository;
import com.vigidrive.vehicleassistant.knowledge.repository.RepairCostRepository;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import com.vigidrive.vehicleassistant.safety.SafetyResult;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VehicleTools {

    private static final Logger log = LoggerFactory.getLogger(VehicleTools.class);

    private final DtcCodeRepository dtcCodeRepository;
    private final ModelInfoRepository modelInfoRepository;
    private final RepairCostRepository repairCostRepository;
    private final SafetyGuard safetyGuard;

    public VehicleTools(DtcCodeRepository dtcCodeRepository,
                        ModelInfoRepository modelInfoRepository,
                        RepairCostRepository repairCostRepository,
                        SafetyGuard safetyGuard) {
        this.dtcCodeRepository = dtcCodeRepository;
        this.modelInfoRepository = modelInfoRepository;
        this.repairCostRepository = repairCostRepository;
        this.safetyGuard = safetyGuard;
    }

    @Tool("Looks up a Diagnostic Trouble Code (DTC) and provides information about it.")
    public String lookupDtc(String dtcCode) {
        log.debug("DTC knowledge lookup requested");
        return dtcCodeRepository.findByCode(dtcCode)
                .map(dtc -> {
                    log.debug("DTC knowledge lookup succeeded");
                    return formatDtc(dtc);
                })
                .orElseGet(() -> {
                    log.warn("DTC knowledge lookup returned no result");
                    return "No reference data found for DTC code " + dtcCode + ".";
                });
    }

    @Tool("Estimates the cost of a repair based on the repair type and vehicle model.")
    public String estimateCost(String repair, String model) {
        log.debug("Repair-cost knowledge lookup requested");
        ModelInfo modelInfo = modelInfoRepository.findByModel(model).orElse(null);
        if (modelInfo == null) {
            log.warn("Repair-cost lookup could not resolve the vehicle model");
            return "No model or segment reference data found for " + model + "; a cost estimate is unavailable.";
        }

        RepairCost cost = repairCostRepository.findByRepairAndSegment(repair, modelInfo.segment()).orElse(null);
        if (cost == null) {
            log.warn("Repair-cost lookup returned no estimate for the vehicle segment");
            return "No reference cost found for " + repair + " on " + model
                    + "; a cost estimate is unavailable.";
        }

        log.debug("Repair-cost knowledge lookup succeeded");
        return "Indicative estimate for " + cost.repair() + " on " + modelInfo.make() + " " + model
                + " (" + modelInfo.segment() + "): INR " + cost.minInr() + "-" + cost.maxInr()
                + ". Source: " + cost.source() + ". As of: " + cost.asOf() + ".";
    }

    @Tool("Checks a driver's message for urgent vehicle hazards. If dangerous, follow the stop-driving instruction.")
    public String checkSafety(String userMessage) {
        SafetyResult result = safetyGuard.check(userMessage);
        if (!result.dangerous()) {
            return "No immediate safety trigger was detected by the rule-based check.";
        }
        return "Urgent safety risk (" + result.reason() + "). " + result.message();
    }

    @Tool("Checks diagnostic trouble codes for codes that require stopping immediately.")
    public String checkDtcSafety(List<String> codes) {
        SafetyResult result = safetyGuard.checkDtcCodes(codes);
        if (!result.dangerous()) {
            return "No stop-now DTC code was detected by the rule-based check.";
        }
        return "Urgent safety risk (" + result.reason() + "). " + result.message();
    }

    private static String formatDtc(DtcCode dtc) {
        return "DTC " + dtc.code() + ": " + dtc.meaning() + ". Severity: " + dtc.severity()
                + ". Common causes: " + String.join("; ", dtc.commonCauses()) + ".";
    }
}
