package com.coldchain.shared.exception;

import com.coldchain.shared.config.web.i18n.Messages;
import com.coldchain.shared.infrastructure.RequestTrace;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetails {

    private static final Map<ErrorCategory, HttpStatus> STATUS_BY_CATEGORY = Map.of(
            ErrorCategory.VALIDATION, HttpStatus.BAD_REQUEST,
            ErrorCategory.AUTHENTICATION, HttpStatus.UNAUTHORIZED,
            ErrorCategory.AUTHORIZATION, HttpStatus.FORBIDDEN,
            ErrorCategory.NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorCategory.CONFLICT, HttpStatus.CONFLICT,
            ErrorCategory.BUSINESS_RULE, HttpStatus.UNPROCESSABLE_CONTENT,
            ErrorCategory.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS,
            ErrorCategory.INTEGRATION, HttpStatus.BAD_GATEWAY,
            ErrorCategory.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR);

    private final Messages messages;

    private final Clock clock;

    public ProblemDetails(Messages messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    public static HttpStatus statusOf(ErrorCategory category) {
        return STATUS_BY_CATEGORY.get(category);
    }

    public ProblemDetail describe(ErrorCode errorCode, String detail, String path) {
        HttpStatus status = statusOf(errorCode.category());
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(ProblemType.from(errorCode.messageKey()));
        String title = messages.of(errorCode.messageKey(), errorCode.title());
        problem.setTitle(title);
        problem.setDetail(detail == null ? title : detail);
        problem.setInstance(URI.create(path));
        problem.setProperty("errorCode", errorCode.name());
        problem.setProperty("messageKey", errorCode.messageKey());
        problem.setProperty("category", errorCode.category().name());
        problem.setProperty("timestamp", clock.instant());
        problem.setProperty("traceId", RequestTrace.current());
        return problem;
    }

    public ProblemDetail describe(ErrorCode errorCode, String detail, String path, List<FieldError> fieldErrors) {
        ProblemDetail problem = describe(errorCode, detail, path);
        problem.setProperty("errors", fieldErrors);
        return problem;
    }
}
