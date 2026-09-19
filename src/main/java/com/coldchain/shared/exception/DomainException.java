package com.coldchain.shared.exception;

import java.util.List;

public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ErrorCode errorCode;

    private final transient List<FieldError> fieldErrors;

    private DomainException(ErrorCode errorCode, String detail, List<FieldError> fieldErrors) {
        super(detail);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public static DomainException of(ErrorCode errorCode) {
        return new DomainException(errorCode, errorCode.title(), List.of());
    }

    public static DomainException of(ErrorCode errorCode, String detail) {
        return new DomainException(errorCode, detail, List.of());
    }

    public static DomainException ofFields(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new DomainException(errorCode, errorCode.title(), fieldErrors);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
