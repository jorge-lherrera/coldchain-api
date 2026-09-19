package com.coldchain.delivery.web.telemetry;

import com.coldchain.delivery.web.telemetry.dto.AssignDeviceRequest;
import com.coldchain.delivery.web.telemetry.dto.AssignmentResponse;
import com.coldchain.delivery.web.telemetry.dto.DeviceResponse;
import com.coldchain.delivery.web.telemetry.dto.IngestBatchRequest;
import com.coldchain.delivery.web.telemetry.dto.IngestBatchResponse;
import com.coldchain.delivery.web.telemetry.dto.RegisterDeviceRequest;
import com.coldchain.delivery.web.telemetry.mapper.TelemetryWebMapper;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.shared.pagination.SortCatalog;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Telemetry", description = "Devices, the batches they send and the series they build")
@RequestMapping("/v1")
public class TelemetryController {

    private static final SortCatalog<DeviceSortField> SORTABLE =
            SortCatalog.of(DeviceSortField.class, DeviceSortField.SERIAL_NUMBER, Sort.Direction.ASC);

    private final TelemetryApi telemetry;

    private final TelemetryWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    private final Clock clock;

    public TelemetryController(TelemetryApi telemetry, TelemetryWebMapper mapper,
            ApiResponseFactory responses, CurrentActor currentActor, Clock clock) {
        this.telemetry = telemetry;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @PostMapping("/devices")
    @Operation(summary = "Register a sensor device",
            description = "The declared sampling interval is what later says how many readings were "
                    + "expected, which is how coverage is judged.")
    @PreAuthorize("hasAuthority('SCOPE_DEVICE_WRITE')")
    public ResponseEntity<ApiResponse<DeviceResponse>> register(
            @Valid @RequestBody RegisterDeviceRequest request) {
        return responses.respond(TelemetrySuccessCode.DEVICE_REGISTERED,
                mapper.toResponse(telemetry.registerDevice(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @GetMapping("/devices")
    @Operation(summary = "List the devices of the organization")
    @PreAuthorize("hasAuthority('SCOPE_DEVICE_READ')")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> list(Pageable pageable) {
        return responses.paginated(TelemetrySuccessCode.DEVICE_LISTED,
                telemetry.listDevices(currentActor.requireOrganizationId(), SORTABLE.apply(pageable))
                        .map(mapper::toResponse));
    }

    @PostMapping("/devices/{deviceId}/assignment")
    @Operation(summary = "Attach a device to a shipment",
            description = "The window it opens is what decides, later, which shipment each reading "
                    + "belongs to — by when it was measured, not by what the device carries today. "
                    + "The thresholds travel with the assignment so telemetry needs to ask nobody.")
    @PreAuthorize("hasAuthority('SCOPE_DEVICE_WRITE')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> assign(@PathVariable UUID deviceId,
            @Valid @RequestBody AssignDeviceRequest request) {
        return responses.respond(TelemetrySuccessCode.DEVICE_ASSIGNED,
                mapper.toResponse(telemetry.assignDevice(mapper.toCommand(deviceId, request))));
    }

    @DeleteMapping("/devices/{deviceId}/assignment")
    @Operation(summary = "Detach a device", description = "Closes the window, it does not delete it.")
    @PreAuthorize("hasAuthority('SCOPE_DEVICE_WRITE')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> detach(@PathVariable UUID deviceId) {
        return responses.respond(TelemetrySuccessCode.DEVICE_DETACHED,
                mapper.toResponse(telemetry.detachDevice(deviceId, clock.instant())));
    }

    @PostMapping("/telemetry/batches")
    @Operation(summary = "Send a batch of readings",
            description = "Idempotent by key: the same batch sent again writes nothing and answers "
                    + "REPLAYED. Readings outside every assignment window, duplicates and impossible "
                    + "values are discarded with a reason, and the rest still go in.")
    @PreAuthorize("hasAuthority('SCOPE_TELEMETRY_INGEST')")
    public ResponseEntity<ApiResponse<IngestBatchResponse>> ingest(
            @Valid @RequestBody IngestBatchRequest request) {
        return responses.respond(TelemetrySuccessCode.BATCH_INGESTED,
                mapper.toResponse(telemetry.ingestBatch(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @GetMapping("/shipments/{shipmentId}/series")
    @Operation(summary = "The temperature series of a shipment and its excursions")
    @PreAuthorize("hasAuthority('SCOPE_TELEMETRY_READ')")
    public ResponseEntity<ApiResponse<SeriesResult>> series(@PathVariable UUID shipmentId) {
        return responses.respond(TelemetrySuccessCode.SERIES_RETRIEVED,
                telemetry.seriesOf(shipmentId, currentActor.requireOrganizationId()));
    }
}
