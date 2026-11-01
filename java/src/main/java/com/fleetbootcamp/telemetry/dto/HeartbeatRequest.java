package com.fleetbootcamp.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/**
 * What a device (or OEM feed) sends to POST /v1/heartbeats.
 *
 * TODO M1 — add Bean Validation (jakarta.validation.constraints.*) so each of these becomes HTTP 400
 * (check with: python3 simulator/simulate.py --mode malformed):
 *   - vin: exactly 17 chars, A-Z and 0-9, never I, O or Q        (@Pattern)
 *   - ts: required; OffsetDateTime already rejects timestamps without a timezone. "Not in the future" -> do it in a service.
 *   - speed_kmph 0..300, lat -90..90, lon -180..180, odometer >= 0, heading 0..359, soc_pct 0..100
 *   - odometer_unit only "km" or "mi"
 * Remember @Valid on the controller parameter. ApiErrors already turns validation failures into the 400 body.
 */
public record HeartbeatRequest(
        @JsonProperty("vin")
        @NotBlank(message = "VIN is required")
        @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Invalid VIN format (must be 17 chars, no I, O, Q)")
        String vin,

        @JsonProperty("ts")
        @NotNull(message = "Timestamp is required")
        OffsetDateTime ts,

        @JsonProperty("speed_kmph")
        @DecimalMin(value = "0.0", message = "Speed must be between 0 and 300")
        @DecimalMax(value = "300.0", message = "Speed must be between 0 and 300")
        Double speedKmph,

        @JsonProperty("odometer")
        @DecimalMin(value = "0.0", message = "Odometer must be >= 0")
        Double odometer,

        @JsonProperty("odometer_unit")
        @Pattern(regexp = "^(km|mi)$", message = "Odometer unit must be km or mi")
        String odometerUnit,

        @JsonProperty("lat")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double lat,

        @JsonProperty("lon")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double lon,

        @JsonProperty("heading")
        @Min(value = 0, message = "Heading must be between 0 and 359")
        @Max(value = 359, message = "Heading must be between 0 and 359")
        Integer heading,

        @JsonProperty("soc_pct")
        @DecimalMin(value = "0.0", message = "SoC must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "SoC must be between 0 and 100")
        Double socPct,

        @JsonProperty("ignition")
        Boolean ignition,

        @JsonProperty("source_oem")
        String sourceOem
) {
}