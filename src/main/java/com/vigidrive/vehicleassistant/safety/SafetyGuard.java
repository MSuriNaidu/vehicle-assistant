package com.vigidrive.vehicleassistant.safety;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SafetyGuard {

    private static final String STOP_MESSAGE = """
            Stop driving. Pull over safely, switch off the engine, and call a tow service.
            Do not drive until a mechanic has inspected the car.

            गाड़ी चलाना रोकें। सुरक्षित जगह पर किनारे लगाएँ, इंजन बंद करें और टो सेवा को बुलाएँ।
            मैकेनिक की जाँच होने तक गाड़ी न चलाएँ।

            వాహనం నడపడం ఆపి, సురక్షితంగా పక్కకు ఆపి, ఇంజిన్ ఆఫ్ చేసి టో సేవకు కాల్ చేయండి.
            మెకానిక్ తనిఖీ చేసే వరకు వాహనం నడపవద్దు.
            """;

    private static final Set<String> DANGEROUS_DTC_CODES = Set.of("P0217");

    // Needs review by a native speaker; add romanised variants as seen in testing.
    private static final String BRAKE_FAILURE_LOCALIZED =
            "(?:ब्रेक\\s*(?:फेल|काम\\s+नहीं\\s+कर(?:ता|ते|रहे)?|नहीं\\s+लग(?:ते|रहे)?|खराब)|" +
            "brakes?\\s+(?:kaam\\s+nahi(?:n)?\\s+kar\\s+rahe|nahi(?:n)?\\s+lag\\s+rahe)|" +
            "(?:బ్రేక్(?:లు)?|బ్రేకులు)\\s*(?:ఫెయిల్|పని\\s+చేయడం\\s+లేదు|పని\\s+చేయటం\\s+లేదు)|" +
            "brakes?\\s+(?:pani\\s+)?(?:cheyatam\\s+ledu|cheyyatledu))";

    // Needs review by a native speaker; add romanised variants as seen in testing.
    private static final String STEERING_LOSS_LOCALIZED =
            "(?:స్టీరింగ్\\s*(?:జామ్|పని\\s+చేయడం\\s+లేదు|స్పందించడం\\s+లేదు)|" +
            "steering\\s+(?:jam\\s+ayindi|panicheyatledu)|" +
            "स्टीयरिंग\\s*(?:जाम|फँस|काम\\s+नहीं\\s+कर|जवाब\\s+नहीं\\s+दे))";

    // Needs review by a native speaker; add romanised variants as seen in testing.
    private static final String OVERHEATING_LOCALIZED =
            "(?:इंजन\\s+गरम\\s+हो\\s+रहा|इंजन\\s+ज़्यादा\\s+गरम|कूलेंट\\s+उबल|भाप\\s+निकल|" +
            "engine\\s+garam\\s+ho\\s+raha|coolant\\s+ubal\\s+raha|" +
            "ఇంజిన్\\s+వేడెక్క|ఇంజిన్\\s+ఎక్కువగా\\s+వేడెక్క|కూలెంట్\\s+మరుగుత|ఆవిరి\\s+వస్త)";

    // Needs review by a native speaker; add romanised variants as seen in testing.
    private static final String FUEL_SMELL_LOCALIZED =
            "(?:पेट्रोल\\s+की\\s+गंध|डीज़ल\\s+की\\s+गंध|ईंधन\\s+(?:की\\s+)?लीक|" +
            "petrol\\s+ki\\s+smell|diesel\\s+ki\\s+smell|fuel\\s+leak\\s+avuthondi|" +
            "పెట్రోల్\\s+వాసన|డీజిల్\\s+వాసన|ఇంధనం\\s+(?:లీక్|కారుతోంది))";

    // Needs review by a native speaker; add romanised variants as seen in testing.
    private static final String SMOKE_OR_FIRE_LOCALIZED =
            "(?:धुआँ\\s+(?:निकल|आ रहा)|जलने\\s+की\\s+गंध|आग\\s+लग|" +
            "dhuan\\s+(?:aa\\s+raha|nikal)|jalne\\s+ki\\s+smell|aag\\s+lag|" +
            "పొగ\\s+(?:వస్త|వెలువడ)|కాలిన\\s+వాసన|మంటలు\\s+(?:వచ్చ|లేచ)|మంటలు\\s+చెలరేగ)";

    private static final List<Rule> RULES = List.of(
            new Rule("BRAKE_FAILURE", compile(
                    "(?:\\bbrakes?\\s+(?:fail(?:ed|ure)?|(?:(?:is|are)\\s+)?not\\s+working|gone|stopped\\s+working)\\b|" +
                    "\\bno\\s+brakes?\\b|\\bspongy\\s+(?:brake\\s+pedal|brakes?)\\b|" +
                    "\\bbrake\\s+pedal\\s+(?:is\\s+)?(?:going\\s+(?:down|to\\s+(?:the\\s+)?floor)|" +
                    "sinking(?:\\s+to\\s+(?:the\\s+)?floor)?|touching\\s+(?:the\\s+)?floor)\\b|" +
                    BRAKE_FAILURE_LOCALIZED + ")")),
            new Rule("STEERING_LOSS", compile(
                    "(?:\\bsteering\\s+(?:wheel\\s+)?(?:is\\s+)?(?:locked|stuck|jammed|not\\s+responding|failed|lost)\\b|" +
                    "\\b(?:lost|failed)\\s+steering\\b|\\b(?:can't|cannot)\\s+steer\\b|\\bunable\\s+to\\s+steer\\b|" +
                    STEERING_LOSS_LOCALIZED + ")")),
            new Rule("OVERHEATING", compile(
                    "(?:\\boverheat(?:ed|ing)?\\b|" +
                    "\\btemperature\\s+(?:needle|gauge)\\s+(?:is\\s+)?in\\s+the\\s+red\\b|" +
                    "\\bcoolant\\s+(?:is\\s+)?boiling\\b|\\bsteam\\s+(?:(?:is\\s+)?coming\\s+)?from\\b|" +
                    OVERHEATING_LOCALIZED + ")")),
            new Rule("FUEL_SMELL", compile(
                    "(?:\\b(?:smell\\s+(?:of\\s+)?(?:petrol|diesel|gas|fuel)|" +
                    "(?:petrol|diesel|gas|fuel)\\s+smell|fuel\\s+leak|leaking\\s+fuel)\\b|" +
                    FUEL_SMELL_LOCALIZED + ")")),
            new Rule("SMOKE_OR_FIRE", compile(
                    "(?:\\b(?<!no\\s)smoke\\s+(?:(?:is\\s+)?coming\\s+)?from\\b|" +
                    "\\bburning\\s+smell\\b|\\bcaught\\s+fire\\b|\\bon\\s+fire\\b|\\bflames?\\b|" +
                    SMOKE_OR_FIRE_LOCALIZED + ")")),
            new Rule("OIL_PRESSURE", compile(
                    "(?:\\boil\\s+pressure\\s+(?:warning\\s+)?light\\b|" +
                    "\\boil\\s+pressure\\s+warning\\b|\\boil\\s+warning\\s+light\\b|" +
                    "\\boil\\s+light\\s+(?:is\\s+)?(?:on|red)\\b)"))
    );

    public SafetyResult check(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return SafetyResult.safe();
        }

        for (Rule rule : RULES) {
            if (rule.pattern().matcher(userMessage).find()) {
                return new SafetyResult(true, rule.name(), STOP_MESSAGE);
            }
        }
        return SafetyResult.safe();
    }

    public SafetyResult checkDtcCodes(List<String> codes) {
        if (codes == null) {
            return SafetyResult.safe();
        }

        for (String code : codes) {
            if (code != null && DANGEROUS_DTC_CODES.contains(code.trim().toUpperCase(Locale.ROOT))) {
                return new SafetyResult(true, "DTC_" + code.trim().toUpperCase(Locale.ROOT), STOP_MESSAGE);
            }
        }
        return SafetyResult.safe();
    }

    private static Pattern compile(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private record Rule(String name, Pattern pattern) {
    }
}
