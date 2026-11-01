package com.fleetbootcamp.telemetry.service;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverspeedRuleTest {

    @Test
    void testNotEnoughPings() {
        assertFalse(OverspeedRule.evaluate(List.of(105.0, 110.0)));
    }

    @Test
    void testExactly100DoesNotTrigger() {
        // Speed must be strictly greater than 100.0
        assertFalse(OverspeedRule.evaluate(List.of(100.0, 105.0, 110.0)));
    }

    @Test
    void testRunOfTwoDoesNotTrigger() {
        assertFalse(OverspeedRule.evaluate(List.of(90.0, 105.0, 110.0)));
    }

    @Test
    void testRunBrokenBySlowPing() {
        assertFalse(OverspeedRule.evaluate(List.of(105.0, 105.0, 95.0, 110.0)));
    }

    @Test
    void testThreeConsecutiveOverspeedTriggers() {
        assertTrue(OverspeedRule.evaluate(List.of(80.0, 105.0, 110.0, 120.0)));
    }
}