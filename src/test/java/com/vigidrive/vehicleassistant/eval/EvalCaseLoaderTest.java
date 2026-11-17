package com.vigidrive.vehicleassistant.eval;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalCaseLoaderTest {

    private static final Set<String> ALLOWED_GROUPS =
            Set.of("DANGEROUS", "NORMAL", "VAGUE", "GENERAL", "TRAP");

    @Test
    void loadsAllThirtyFourCasesWithUniqueIdsAndKnownGroups() {
        var cases = EvalCaseLoader.load();

        assertEquals(34, cases.size());
        assertEquals(cases.size(), cases.stream().map(EvalCase::id).collect(Collectors.toSet()).size());
        assertTrue(cases.stream().allMatch(evalCase -> ALLOWED_GROUPS.contains(evalCase.group())));
    }

    @Test
    void defaultsOptionalListsAndFollowUpWhenMissing() {
        var cases = EvalCaseLoader.load("""
                [{"id":"MINIMAL","group":"GENERAL","language":"en","input":"hello"}]
                """);

        assertEquals(java.util.List.of(), cases.getFirst().acceptableUrgency());
        assertEquals(java.util.List.of(), cases.getFirst().mustMention());
        assertEquals(java.util.List.of(), cases.getFirst().mustNotContain());
        assertEquals(Boolean.FALSE, cases.getFirst().expectFollowUp());
    }

    @Test
    void reportsDuplicateIds() {
        var exception = assertThrows(IllegalArgumentException.class, () -> EvalCaseLoader.load("""
                [
                  {"id":"DUP","group":"GENERAL","language":"en","input":"one"},
                  {"id":"DUP","group":"GENERAL","language":"en","input":"two"}
                ]
                """));

        assertTrue(exception.getMessage().contains("DUP"));
    }

    @Test
    void reportsUnknownUrgencyWithCaseId() {
        var exception = assertThrows(IllegalArgumentException.class, () -> EvalCaseLoader.load("""
                [{"id":"BAD-U","group":"GENERAL","language":"en","input":"hello",
                  "acceptableUrgency":["PANIC"]}]
                """));

        assertTrue(exception.getMessage().contains("BAD-U"));
        assertTrue(exception.getMessage().contains("PANIC"));
    }

    @Test
    void reportsMalformedCaseWithItsId() {
        var exception = assertThrows(IllegalArgumentException.class, () -> EvalCaseLoader.load("""
                [{"id":"BAD-CASE","group":"GENERAL","language":"en","input":{"unexpected":"object"}}]
                """));

        assertTrue(exception.getMessage().contains("BAD-CASE"));
    }
}
