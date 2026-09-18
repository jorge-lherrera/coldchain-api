package com.coldchain.delivery.web.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record RegisterDeviceRequest(
        @NotBlank @Size(max = 60) String serialNumber,
        @NotBlank @Size(max = 80) String model,
        @NotBlank @Size(max = 40) String firmware,
        @Positive int samplingIntervalSeconds,
        @NotNull Instant calibratedAt) {
}
