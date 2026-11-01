package com.fleetbootcamp.telemetry.repo;

import com.fleetbootcamp.telemetry.dto.AlertRecord;
import com.fleetbootcamp.telemetry.dto.FleetLatestRecord;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class TelemetryRepository {
    private final JdbcTemplate jdbc;

    public TelemetryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean vehicleExists(String vin) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE vin = ?", Integer.class, vin);
        return n != null && n > 0;
    }

    public boolean insert(TelemetryEvent e) {
        String sql = "INSERT INTO telemetry (vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (vin, ts_utc) DO NOTHING";
        
        int rowsAffected = jdbc.update(sql,
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

    private TelemetryEvent mapRowToTelemetry(ResultSet rs, int rowNum) throws SQLException {
        Instant ts = rs.getTimestamp("ts_utc") != null ? rs.getTimestamp("ts_utc").toInstant() : null;
        return new TelemetryEvent(
                null,
                rs.getString("vin"),
                ts,
                rs.getDouble("speed_kmph"),
                rs.getObject("odometer_km") != null ? rs.getDouble("odometer_km") : null,
                rs.getObject("lat") != null ? rs.getDouble("lat") : null,
                rs.getObject("lon") != null ? rs.getDouble("lon") : null,
                rs.getObject("heading") != null ? rs.getInt("heading") : null,
                rs.getObject("soc_pct") != null ? rs.getDouble("soc_pct") : null,
                rs.getObject("ignition") != null ? rs.getBoolean("ignition") : null,
                rs.getString("source_oem")
        );
    }

    public TelemetryEvent latest(String vin) {
        String sql = "SELECT vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem " +
                     "FROM telemetry WHERE vin = ? ORDER BY ts_utc DESC LIMIT 1";
        List<TelemetryEvent> results = jdbc.query(sql, this::mapRowToTelemetry, vin);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<TelemetryEvent> range(String vin, Instant from, Instant to, int limit, Instant cursor) {
        StringBuilder sql = new StringBuilder(
            "SELECT vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem " +
            "FROM telemetry WHERE vin = ? AND ts_utc >= ? AND ts_utc <= ?"
        );
        
        Object[] params;
        if (cursor != null) {
            sql.append(" AND ts_utc < ?");
            sql.append(" ORDER BY ts_utc DESC LIMIT ?");
            params = new Object[]{vin, from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC), cursor.atOffset(ZoneOffset.UTC), limit};
        } else {
            sql.append(" ORDER BY ts_utc DESC LIMIT ?");
            params = new Object[]{vin, from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC), limit};
        }

        return jdbc.query(sql.toString(), this::mapRowToTelemetry, params);
    }

    public List<AlertRecord> alerts(String vin) {
        String sql = "SELECT id, vin, alert_type, ts_utc, details FROM alerts WHERE vin = ? ORDER BY ts_utc DESC";
        return jdbc.query(sql, (rs, rowNum) -> new AlertRecord(
                rs.getLong("id"),
                rs.getString("vin"),
                rs.getString("alert_type"),
                rs.getTimestamp("ts_utc") != null ? rs.getTimestamp("ts_utc").toInstant() : null,
                rs.getString("details")
        ), vin);
    }

    public List<FleetLatestRecord> fleetLatest(int customerId) {
        String sql = "SELECT t.vin, t.ts_utc, t.speed_kmph, t.odometer_km, t.lat, t.lon, t.heading, t.soc_pct, t.ignition, t.source_oem " +
                     "FROM vehicles v " +
                     "JOIN LATERAL (" +
                     "  SELECT * FROM telemetry WHERE vin = v.vin ORDER BY ts_utc DESC LIMIT 1" +
                     ") t ON true " +
                     "WHERE v.customer_id = ?";
        return jdbc.query(sql, (rs, rowNum) -> new FleetLatestRecord(
                rs.getString("vin"),
                rs.getTimestamp("ts_utc") != null ? rs.getTimestamp("ts_utc").toInstant() : null,
                rs.getDouble("speed_kmph"),
                rs.getObject("odometer_km") != null ? rs.getDouble("odometer_km") : null,
                rs.getObject("lat") != null ? rs.getDouble("lat") : null,
                rs.getObject("lon") != null ? rs.getDouble("lon") : null,
                rs.getObject("heading") != null ? rs.getInt("heading") : null,
                rs.getObject("soc_pct") != null ? rs.getDouble("soc_pct") : null,
                rs.getObject("ignition") != null ? rs.getBoolean("ignition") : null,
                rs.getString("source_oem")
        ), customerId);
    }

    public boolean insertAlert(String vin, String alertType, Instant tsUtc, String details) {
        String sql = "INSERT INTO alerts (vin, alert_type, ts_utc, details) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT DO NOTHING";
        int rowsAffected = jdbc.update(sql, vin, alertType, tsUtc.atOffset(ZoneOffset.UTC), details);
        return rowsAffected > 0;
    }

    public List<Double> findRecentSpeeds(String vin, int limit) {
        String sql = "SELECT speed_kmph FROM telemetry WHERE vin = ? ORDER BY ts_utc DESC LIMIT ?";
        return jdbc.queryForList(sql, Double.class, vin, limit);
    }
}