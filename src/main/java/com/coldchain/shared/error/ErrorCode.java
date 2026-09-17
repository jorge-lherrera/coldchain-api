package com.coldchain.shared.error;

public interface ErrorCode {

    String name();

    String messageKey();

    ErrorCategory category();

    String title();
}
