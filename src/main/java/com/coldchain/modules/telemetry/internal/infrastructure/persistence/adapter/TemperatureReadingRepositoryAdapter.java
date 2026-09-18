package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import com.coldchain.modules.telemetry.internal.domain.repository.TemperatureReadingRepository;
import com.coldchain.shared.identifier.RawUuid;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TemperatureReadingRepositoryAdapter implements TemperatureReadingRepository {

    private static final String INSERT = """
            INSERT INTO TEMPERATURE_READING
                (ID, ORGANIZATION_ID, DEVICE_ID, SHIPMENT_ID, BATCH_ID, CELSIUS, MEASURED_AT,
                 CREATED_AT, CREATED_BY)
            VALUES (?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?)
            """;

    private static final String SELECT_SERIES = """
            SELECT ID, ORGANIZATION_ID, DEVICE_ID, SHIPMENT_ID, BATCH_ID, CELSIUS, MEASURED_AT
            FROM TEMPERATURE_READING
            WHERE SHIPMENT_ID = ?
            ORDER BY MEASURED_AT
            """;

    private static final String SELECT_SAMPLES = """
            SELECT MEASURED_AT FROM TEMPERATURE_READING
            WHERE DEVICE_ID = ? AND MEASURED_AT BETWEEN ? AND ?
            """;

    private static final int[] INSERT_TYPES = {Types.BINARY, Types.BINARY, Types.BINARY, Types.BINARY,
            Types.BINARY, Types.DECIMAL, Types.TIMESTAMP_WITH_TIMEZONE, Types.BINARY};

    private final JdbcTemplate jdbc;

    public TemperatureReadingRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int saveAll(List<TemperatureReading> readings) {
        if (readings.isEmpty()) {
            return 0;
        }
        List<Object[]> rows = readings.stream()
                .map(reading -> new Object[] {
                        RawUuid.toBytes(reading.id()),
                        RawUuid.toBytes(reading.organizationId()),
                        RawUuid.toBytes(reading.deviceId()),
                        RawUuid.toBytes(reading.shipmentId()),
                        RawUuid.toBytes(reading.batchId()),
                        reading.celsius(),
                        Timestamp.from(reading.measuredAt()),
                        RawUuid.toBytes(reading.organizationId())})
                .toList();
        return jdbc.batchUpdate(INSERT, rows, INSERT_TYPES).length;
    }

    @Override
    public List<TemperatureReading> findOfShipment(UUID shipmentId) {
        return jdbc.query(SELECT_SERIES,
                (row, index) -> TemperatureReading.restore(
                        RawUuid.fromBytes(row.getBytes("ID")),
                        RawUuid.fromBytes(row.getBytes("ORGANIZATION_ID")),
                        RawUuid.fromBytes(row.getBytes("DEVICE_ID")),
                        RawUuid.fromBytes(row.getBytes("SHIPMENT_ID")),
                        RawUuid.fromBytes(row.getBytes("BATCH_ID")),
                        row.getBigDecimal("CELSIUS"),
                        row.getTimestamp("MEASURED_AT").toInstant()),
                RawUuid.toBytes(shipmentId));
    }

    @Override
    public List<Instant> findMeasuredAtOf(UUID deviceId, Instant from, Instant to) {
        return jdbc.query(SELECT_SAMPLES,
                (row, index) -> row.getTimestamp("MEASURED_AT").toInstant(),
                RawUuid.toBytes(deviceId), Timestamp.from(from), Timestamp.from(to));
    }
}
