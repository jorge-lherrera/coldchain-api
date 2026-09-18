package com.coldchain.delivery.web.telemetry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record IngestBatchRequest(
        @NotNull UUID deviceId,
        @NotBlank @Size(max = 80) String idempotencyKey,
        @NotEmpty @Valid List<ReadingPayload> readings) {
}
