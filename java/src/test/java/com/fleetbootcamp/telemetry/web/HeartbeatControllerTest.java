package com.fleetbootcamp.telemetry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.service.IngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HeartbeatController.class)
class HeartbeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestService ingestService;

    @Test
    void ingest_validRequest_returnsAccepted() throws Exception {
        HeartbeatRequest request = new HeartbeatRequest(
                "1HGCR2F83HA000001",
                OffsetDateTime.now(),
                50.0,
                1000.0,
                "km",
                12.9716,
                77.5946,
                180,
                85.0,
                true,
                "OEM-A"
        );

        TelemetryEvent mockEvent = new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                "1HGCR2F83HA000001",
                Instant.now(),
                50.0,
                1000.0,
                12.9716,
                77.5946,
                180,
                85.0,
                true,
                "OEM-A"
        );

        when(ingestService.process(any())).thenReturn(mockEvent);

        mockMvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.vin").value("1HGCR2F83HA000001"));
    }

    @Test
    void ingest_invalidVin_returnsBadRequest() throws Exception {
        HeartbeatRequest request = new HeartbeatRequest(
                "INVALIDVIN",
                OffsetDateTime.now(),
                50.0,
                1000.0,
                "km",
                12.9716,
                77.5946,
                180,
                85.0,
                true,
                "OEM-A"
        );

        mockMvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}