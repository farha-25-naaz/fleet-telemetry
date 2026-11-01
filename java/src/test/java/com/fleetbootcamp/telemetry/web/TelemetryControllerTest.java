package com.fleetbootcamp.telemetry.web;

import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelemetryRepository telemetryRepository;

    @Test
    void testGetAlertsEndpoint() throws Exception {
        when(telemetryRepository.alerts(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/vehicles/TESTVIN123/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetFleetLatestEndpoint() throws Exception {
        when(telemetryRepository.fleetLatest(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/fleets/1/latest"))
                .andExpect(status().isOk());
    }
}