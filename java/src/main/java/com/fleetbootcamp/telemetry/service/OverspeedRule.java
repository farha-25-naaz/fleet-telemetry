package com.fleetbootcamp.telemetry.service;

import java.util.List;

/**
 * Pure function rule engine to evaluate overspeed conditions.
 * Rule: speed > 100 km/h for 3 consecutive pings.
 */
public final class OverspeedRule {

    private OverspeedRule() {
    }

    public static boolean evaluate(List<Double> speeds) {
        if (speeds == null || speeds.size() < 3) {
            return false;
        }
        int n = speeds.size();
        return speeds.get(n - 1) > 100.0
                && speeds.get(n - 2) > 100.0
                && speeds.get(n - 3) > 100.0;
    }
}