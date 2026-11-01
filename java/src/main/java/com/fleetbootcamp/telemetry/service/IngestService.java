package com.fleetbootcamp.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.error.ApiErrors;
import com.fleetbootcamp.telemetry.kafka.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class IngestService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public IngestService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public TelemetryEvent process(HeartbeatRequest request) {
        // 1. Reject timestamps more than 5 minutes in the future
        if (request.ts() != null && request.ts().isAfter(OffsetDateTime.now().plus(5, ChronoUnit.MINUTES))) {
            throw new ApiErrors.ValidationFailedException("ts", "Timestamp cannot be in the future");
        }

        // 2. Build a TelemetryEvent with Normalizer.normalize(...)
        TelemetryEvent event = Normalizer.normalize(request);

        // 3. Send as JSON to Kafka using VIN as key (keeping all events for one vehicle in order)
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = request.vin();
            kafkaTemplate.send(Topics.RAW, key, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize telemetry event", e);
        }

        return event;
    }
}