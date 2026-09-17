package com.coldchain.modules.identity.api.dto;

public record AuthenticateCommand(String email, String password) {
}
