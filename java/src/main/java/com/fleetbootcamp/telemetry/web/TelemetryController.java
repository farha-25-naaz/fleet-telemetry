package com.fleetbootcamp.telemetry.web;

import com.fleetbootcamp.telemetry.dto.AlertRecord;
import com.fleetbootcamp.telemetry.dto.FleetLatestRecord;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class TelemetryController {

    private final TelemetryRepository repository;

    public TelemetryController(TelemetryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/vehicles/{vin}/latest")
    public ResponseEntity<TelemetryEvent> getLatest(@PathVariable String vin) {
        TelemetryEvent event = repository.latest(vin.toUpperCase());
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/vehicles/{vin}/telemetry")
    public ResponseEntity<List<TelemetryEvent>> getRange(
            @PathVariable String vin,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Instant cursor) {
        
        List<TelemetryEvent> results = repository.range(vin.toUpperCase(), from, to, limit, cursor);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/vehicles/{vin}/alerts")
    public ResponseEntity<List<AlertRecord>> getAlerts(@PathVariable String vin) {
        List<AlertRecord> alerts = repository.alerts(vin.toUpperCase());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/fleets/{customer_id}/latest")
    public ResponseEntity<List<FleetLatestRecord>> getFleetLatest(@PathVariable("customer_id") int customerId) {
        List<FleetLatestRecord> fleetLatest = repository.fleetLatest(customerId);
        return ResponseEntity.ok(fleetLatest);
    }
}