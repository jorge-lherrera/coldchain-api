package com.coldchain.modules.identity.api.dto;

public record IssueClientTokenCommand(String clientId, String clientSecret) {
}
