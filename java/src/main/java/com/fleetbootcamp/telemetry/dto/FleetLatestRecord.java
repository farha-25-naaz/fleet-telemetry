package com.fleetbootcamp.telemetry.dto;

import java.time.Instant;

public record FleetLatestRecord(
    String vin,
    Instant tsUtc,
    double speedKmph,
    Double odometerKm,
    Double lat,
    Double lon,
    Integer heading,
    Double socPct,
    Boolean ignition,
    String sourceOem
) {}