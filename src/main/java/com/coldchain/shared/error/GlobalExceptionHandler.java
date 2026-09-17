package com.coldchain.shared.error;

import com.coldchain.shared.observability.LogMessage;
import com.coldchain.shared.trace.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    public static HttpStatus statusOf(ErrorCategory category) {
        return STATUS_BY_CATEGORY.get(category);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> onDomainException(DomainException exception, HttpServletRequest request) {
        ProblemDetail problem = describe(exception.errorCode(), exception.getMessage(), request.getRequestURI());
        if (!exception.fieldErrors().isEmpty()) {
            problem.setProperty("errors", exception.fieldErrors());
        }
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> onAuthenticationFailure(AuthenticationException exception,
            HttpServletRequest request) {
        return respond(CoreErrorCode.NOT_AUTHENTICATED, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> onAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return respond(CoreErrorCode.NOT_AUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> onConcurrentModification(HttpServletRequest request) {
        return respond(CoreErrorCode.CONCURRENT_MODIFICATION, null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> onUntranslatedConstraint(DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOG.error(LogMessage.UNTRANSLATED_CONSTRAINT, exception.getMostSpecificCause().getMessage(), exception);
        return respond(CoreErrorCode.UNEXPECTED_FAILURE, null, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> onUnexpectedFailure(Exception exception, HttpServletRequest request) {
        LOG.error(LogMessage.UNEXPECTED_FAILURE, exception);
        return respond(CoreErrorCode.UNEXPECTED_FAILURE, null, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = describe(CoreErrorCode.VALIDATION_FAILED, null, pathOf(request));
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = status.is5xxServerError()
                ? CoreErrorCode.UNEXPECTED_FAILURE
                : CoreErrorCode.MALFORMED_REQUEST;
        ProblemDetail problem = describe(errorCode, null, pathOf(request));
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problem = describe(errorCode, detail, request.getRequestURI());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private ProblemDetail describe(ErrorCode errorCode, String detail, String path) {
        HttpStatus status = statusOf(errorCode.category());
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(ProblemType.from(errorCode.messageKey()));
        problem.setTitle(errorCode.title());
        problem.setDetail(detail == null ? errorCode.title() : detail);
        problem.setInstance(URI.create(path));
        problem.setProperty("errorCode", errorCode.name());
        problem.setProperty("messageKey", errorCode.messageKey());
        problem.setProperty("category", errorCode.category().name());
        problem.setProperty("timestamp", clock.instant());
        problem.setProperty("traceId", RequestTrace.current());
        return problem;
    }

    private static String pathOf(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
