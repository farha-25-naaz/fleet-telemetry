package com.fleetbootcamp.telemetry.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final ObjectMapper objectMapper;
    private final TelemetryRepository telemetryRepository;

    public TelemetryConsumer(ObjectMapper objectMapper, TelemetryRepository telemetryRepository) {
        this.objectMapper = objectMapper;
        this.telemetryRepository = telemetryRepository;
    }

    @KafkaListener(topics = Topics.RAW)
    @Transactional
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("partition={} offset={} key={} value={}", record.partition(), record.offset(), record.key(), record.value());

        try {
            TelemetryEvent event = objectMapper.readValue(record.value(), TelemetryEvent.class);

            if (!TelemetryEvent.SCHEMA_V1.equals(event.schema())) {
                throw new IllegalArgumentException("Invalid schema version: " + event.schema());
            }

            if (event.vin() == null || event.vin().isBlank()) {
                throw new IllegalArgumentException("Missing required field: vin");
            }

            telemetryRepository.insert(event);
            log.info("Successfully persisted telemetry event for VIN: {}", event.vin());

        } catch (Exception e) {
            log.error("Failed to process Kafka record at offset {}: {}", record.offset(), e.getMessage());
            throw new RuntimeException("Failed processing telemetry record", e);
        }
    }
}