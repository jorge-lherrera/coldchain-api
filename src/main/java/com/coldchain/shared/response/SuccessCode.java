package com.coldchain.shared.response;

public interface SuccessCode {

    String name();

    String messageKey();

    SuccessOutcome outcome();

    String message();
}
