package com.vigidrive.vehicleassistant.safety;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafetyGuardTest {

    private final SafetyGuard safetyGuard = new SafetyGuard();

    @ParameterizedTest
    @MethodSource("safetyMessages")
    void detectsOnlySpecificSafetyPhrases(String message, boolean expectedDangerous) {
        assertEquals(expectedDangerous, safetyGuard.check(message).dangerous(), message);
    }

    static Stream<Arguments> safetyMessages() {
        return Stream.of(
                Arguments.of("My brakes failed on the highway", true),
                Arguments.of("The brake is not working", true),
                Arguments.of("No brakes at all", true),
                Arguments.of("I have a spongy brake pedal", true),
                Arguments.of("The brake pedal is sinking to the floor", true),
                Arguments.of("ब्रेक काम नहीं कर रहे हैं", true),
                Arguments.of("బ్రేకులు పని చేయడం లేదు", true),
                Arguments.of("brakes pani cheyatam ledu", true),

                Arguments.of("The steering wheel is locked", true),
                Arguments.of("My steering is jammed", true),
                Arguments.of("The steering is not responding", true),
                Arguments.of("I can't steer the car", true),
                Arguments.of("स्टीयरिंग जाम हो गया", true),
                Arguments.of("స్టీరింగ్ స్పందించడం లేదు", true),
                Arguments.of("steering jam ayindi", true),

                Arguments.of("The engine is overheating", true),
                Arguments.of("The temperature gauge is in the red", true),
                Arguments.of("Coolant is boiling", true),
                Arguments.of("Steam is coming from under the hood", true),
                Arguments.of("इंजन गरम हो रहा है", true),
                Arguments.of("ఇంజిన్ వేడెక్కుతోంది", true),
                Arguments.of("engine garam ho raha hai", true),

                Arguments.of("I smell petrol inside the car", true),
                Arguments.of("There is a smell of diesel", true),
                Arguments.of("I can smell gas", true),
                Arguments.of("There is a fuel leak", true),
                Arguments.of("पेट्रोल की गंध आ रही है", true),
                Arguments.of("పెట్రోల్ వాసన వస్తోంది", true),
                Arguments.of("petrol ki smell aa rahi hai", true),

                Arguments.of("Smoke is coming from the engine", true),
                Arguments.of("There is a burning smell", true),
                Arguments.of("The car caught fire", true),
                Arguments.of("The vehicle is on fire", true),
                Arguments.of("I can see flames under the hood", true),
                Arguments.of("धुआँ निकल रहा है", true),
                Arguments.of("పొగ వస్తోంది", true),
                Arguments.of("aag lag gayi", true),

                Arguments.of("The oil pressure warning light came on", true),
                Arguments.of("The oil light is red", true),
                Arguments.of("The oil light is on", true),

                Arguments.of("brakes are fine", false),
                Arguments.of("got my brakes serviced yesterday", false),
                Arguments.of("smoke test passed at the service centre", false),
                Arguments.of("engine light is on and the car jerks at 60 km/h", false),
                Arguments.of("car makes noise", false),
                Arguments.of("The mechanic inspected the brakes yesterday", false),
                Arguments.of("The temperature gauge is normal", false),
                Arguments.of("I changed the oil last week", false),
                Arguments.of("The car has a new steering wheel", false),
                Arguments.of("Fuel economy has decreased", false),
                Arguments.of("There is no smoke from the car", false)
        );
    }

    @org.junit.jupiter.api.Test
    void treatsNullAndBlankMessagesAsSafe() {
        assertEquals(SafetyResult.safe(), safetyGuard.check(null));
        assertEquals(SafetyResult.safe(), safetyGuard.check(" \t "));
    }

    @org.junit.jupiter.api.Test
    void flagsOnlyConfiguredStopNowDtcCodes() {
        assertEquals(true, safetyGuard.checkDtcCodes(List.of("p0217")).dangerous());
        assertEquals(false, safetyGuard.checkDtcCodes(List.of("P0300", "P0420")).dangerous());
        assertEquals(false, safetyGuard.checkDtcCodes(null).dangerous());
    }
}
