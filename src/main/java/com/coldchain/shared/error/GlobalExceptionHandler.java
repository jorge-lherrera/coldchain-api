package com.coldchain.shared.error;

import com.coldchain.shared.observability.LogMessage;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
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

    private final ProblemDetails problemDetails;

    public GlobalExceptionHandler(ProblemDetails problemDetails) {
        this.problemDetails = problemDetails;
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> onDomainException(DomainException exception, HttpServletRequest request) {
        ProblemDetail problem = exception.fieldErrors().isEmpty()
                ? problemDetails.describe(exception.errorCode(), exception.getMessage(), request.getRequestURI())
                : problemDetails.describe(exception.errorCode(), exception.getMessage(),
                        request.getRequestURI(), exception.fieldErrors());
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
        ProblemDetail problem =
                problemDetails.describe(CoreErrorCode.VALIDATION_FAILED, null, pathOf(request), fieldErrors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = status.is5xxServerError()
                ? CoreErrorCode.UNEXPECTED_FAILURE
                : CoreErrorCode.MALFORMED_REQUEST;
        ProblemDetail problem = problemDetails.describe(errorCode, null, pathOf(request));
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problem = problemDetails.describe(errorCode, detail, request.getRequestURI());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static String pathOf(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
