package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceAssignmentRepository {

    DeviceAssignment save(DeviceAssignment assignment);

    List<DeviceAssignment> findWindowsOf(UUID deviceId);

    Optional<DeviceAssignment> findOpen(UUID deviceId);
}
