package com.fleetbootcamp.telemetry.repo;

import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;

/** All SQL lives here. Always use ? placeholders — never concatenate input into SQL. */
@Repository
public class TelemetryRepository {
    private final JdbcTemplate jdbc;

    public TelemetryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Example of the style to follow. */
    public boolean vehicleExists(String vin) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE vin = ?", Integer.class, vin);
        return n != null && n > 0;
    }

    public boolean insert(TelemetryEvent e) {
        String sql = "INSERT INTO telemetry (schema, vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (vin, ts_utc) DO NOTHING";
        
        int rowsAffected = jdbc.update(sql,
                e.schema(),
                e.vin(),
                e.tsUtc().atOffset(ZoneOffset.UTC),
                e.speedKmph(),
                e.odometerKm(),
                e.lat(),
                e.lon(),
                e.heading(),
                e.socPct(),
                e.ignition(),
                e.sourceOem()
        );
        return rowsAffected > 0;
    }

    // TODO M3: latest(vin), range(vin, from, to, limit, cursor) with keyset pagination, alerts(vin),
    //          fleetLatest(customerId) as ONE query. Check your range query with EXPLAIN in psql.
}