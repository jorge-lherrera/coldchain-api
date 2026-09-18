package com.coldchain.delivery.web.identity;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum IdentitySuccessCode implements SuccessCode {

    ORGANIZATION_REGISTERED("success.identity.organization_registered", SuccessOutcome.CREATED,
            "Organization registered"),
    USER_INVITED("success.identity.user_invited", SuccessOutcome.CREATED, "User invited"),
    USER_ACTIVATED("success.identity.user_activated", SuccessOutcome.UPDATED, "User activated"),
    USER_LISTED("success.identity.user_listed", SuccessOutcome.RETRIEVED, "Users retrieved"),
    TOKEN_ISSUED("success.identity.token_issued", SuccessOutcome.CREATED, "Token issued"),
    ROLE_ASSIGNED("success.identity.role_assigned", SuccessOutcome.CREATED, "Role assigned"),
    ROLE_REVOKED("success.identity.role_revoked", SuccessOutcome.DELETED, "Role revoked"),
    API_CLIENT_CREATED("success.identity.api_client_created", SuccessOutcome.CREATED,
            "Machine credential created"),
    SCOPES_RETRIEVED("success.identity.scopes_retrieved", SuccessOutcome.RETRIEVED, "Scopes retrieved");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    IdentitySuccessCode(String messageKey, SuccessOutcome outcome, String message) {
        this.messageKey = messageKey;
        this.outcome = outcome;
        this.message = message;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    @Override
    public SuccessOutcome outcome() {
        return outcome;
    }

    @Override
    public String message() {
        return message;
    }
}
