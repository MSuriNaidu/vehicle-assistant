package com.vigidrive.vehicleassistant.knowledge;

import java.util.List;

public record DtcCode(String code, String meaning, DtcSeverity severity, List<String> commonCauses) {
}
