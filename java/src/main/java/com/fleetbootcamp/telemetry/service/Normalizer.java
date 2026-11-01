package com.fleetbootcamp.telemetry.service;

import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;

/** Device heartbeat -> normalised TelemetryEvent (metric units, UTC). Keep it pure so it's easy to unit-test. */
public final class Normalizer {
    public static final double KM_PER_MILE = 1.609344;

    private Normalizer() {
    }

    public static TelemetryEvent normalize(HeartbeatRequest hb) {
        // 1. Calculate odometer in km
        Double odometerKm = hb.odometer();
        if (odometerKm != null && "mi".equalsIgnoreCase(hb.odometerUnit())) {
            odometerKm = odometerKm * KM_PER_MILE;
        }

        // 2. Build and return the normalized TelemetryEvent
        return new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                hb.vin() != null ? hb.vin().toUpperCase() : null,
                hb.ts() != null ? hb.ts().toInstant() : null,
                hb.speedKmph(),
                odometerKm,
                hb.lat(),
                hb.lon(),
                hb.heading(),
                hb.socPct(),
                hb.ignition(),
                hb.sourceOem()
        );
    }
}