package com.coldchain.shared.response;

import com.coldchain.shared.config.web.i18n.Messages;
import com.coldchain.shared.infrastructure.RequestTrace;
import com.coldchain.shared.pagination.PagedResult;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    private static final Map<SuccessOutcome, HttpStatus> STATUS_BY_OUTCOME = Map.of(
            SuccessOutcome.CREATED, HttpStatus.CREATED,
            SuccessOutcome.RETRIEVED, HttpStatus.OK,
            SuccessOutcome.UPDATED, HttpStatus.OK,
            SuccessOutcome.DELETED, HttpStatus.OK,
            SuccessOutcome.ACCEPTED, HttpStatus.ACCEPTED);

    private final Messages messages;

    private final Clock clock;

    public ApiResponseFactory(Messages messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    public <T> ResponseEntity<ApiResponse<T>> respond(SuccessCode code, T data) {
        return build(code, data, ResponseMeta.empty());
    }

    public <T> ResponseEntity<ApiResponse<List<T>>> paginated(SuccessCode code, PagedResult<T> page) {
        return build(code, page.content(), ResponseMeta.paginated(PaginationMeta.of(page)));
    }

    public static HttpStatus statusOf(SuccessOutcome outcome) {
        return STATUS_BY_OUTCOME.get(outcome);
    }

    private <T> ResponseEntity<ApiResponse<T>> build(SuccessCode code, T data, ResponseMeta meta) {
        HttpStatus status = statusOf(code.outcome());
        ApiResponse<T> body = new ApiResponse<>(
                status.value(),
                code.name(),
                code.messageKey(),
                messages.of(code.messageKey(), code.message()),
                data,
                RequestTrace.current(),
                clock.instant(),
                meta);
        return ResponseEntity.status(status).body(body);
    }
}
