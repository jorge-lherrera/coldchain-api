package com.coldchain.shared.exception;

public interface ErrorCode {

    String name();

    String messageKey();

    ErrorCategory category();

    String title();
}
