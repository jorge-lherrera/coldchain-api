package com.coldchain.modules.telemetry.internal.domain.service;

import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class AssignmentWindowResolver {

    private AssignmentWindowResolver() {
    }

    public static Optional<DeviceAssignment> resolve(List<DeviceAssignment> windows,
            Instant measuredAt) {
        return windows.stream().filter(window -> window.covers(measuredAt)).findFirst();
    }
}
