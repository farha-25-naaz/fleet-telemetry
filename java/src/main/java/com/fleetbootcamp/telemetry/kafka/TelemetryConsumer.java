package com.fleetbootcamp.telemetry.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import com.fleetbootcamp.telemetry.service.OverspeedRule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final ObjectMapper objectMapper;
    private final TelemetryRepository telemetryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TelemetryConsumer(ObjectMapper objectMapper, 
                             TelemetryRepository telemetryRepository,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.telemetryRepository = telemetryRepository;
        this.kafkaTemplate = kafkaTemplate;
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

            boolean inserted = telemetryRepository.insert(event);
            if (inserted) {
                log.info("Successfully persisted telemetry event for VIN: {}", event.vin());

                // Check overspeed rule (requires last 3 pings)
                List<Double> recentSpeeds = telemetryRepository.findRecentSpeeds(event.vin(), 3);
                if (OverspeedRule.evaluate(recentSpeeds)) {
                    String alertDetails = "Speed exceeded 100 km/h for 3 consecutive pings. Current speed: " + event.speedKmph();
                    
                    // Insert alert (handled safely if unique constraint prevents duplicates for the same timestamp)
                    boolean alertInserted = telemetryRepository.insertAlert(
                            event.vin(), 
                            "OVERSPEED", 
                            event.tsUtc(), 
                            alertDetails
                    );

                    if (alertInserted) {
                        log.warn("OVERSPEED ALERT triggered for VIN: {}", event.vin());
                        
                        // Publish to alerts.out topic
                        Map<String, Object> alertPayload = Map.of(
                                "vin", event.vin(),
                                "alert_type", "OVERSPEED",
                                "ts_utc", event.tsUtc().toString(),
                                "details", alertDetails
                        );
                        String alertJson = objectMapper.writeValueAsString(alertPayload);
                        kafkaTemplate.send("alerts.out", event.vin(), alertJson);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to process Kafka record at offset {}: {}", record.offset(), e.getMessage());
            throw new RuntimeException("Failed processing telemetry record", e);
        }
    }
}