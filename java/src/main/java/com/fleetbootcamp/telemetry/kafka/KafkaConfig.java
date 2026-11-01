package com.fleetbootcamp.telemetry.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * TODO M2: define a DefaultErrorHandler @Bean here. Spring Boot plugs it into the listener automatically.
 *   - unparseable / wrong-schema messages: no retry, publish to telemetry.dlq (DeadLetterPublishingRecoverer)
 *   - other errors (DB briefly down): a few retries with a back-off, then DLQ
 *   Without this, Spring retries 10 times and then just logs and skips — the message is silently lost.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (consumerRecord, exception) -> new org.apache.kafka.common.TopicPartition(Topics.DLQ, consumerRecord.partition()));
        
        // Retry transient errors with exponential back-off (e.g., starting at 1s, max 10s total)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Do not retry invalid or unparseable payloads (go straight to DLQ)
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}