package com.coldchain.modules.identity.api.dto;

public record ActivateUserCommand(String activationToken, String password) {
}
