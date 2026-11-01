package com.fleetbootcamp.telemetry.dto;

import java.time.Instant;

public record AlertRecord(
    Long id,
    String vin,
    String alertType,
    Instant tsUtc,
    String details
) {}