package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.RoleCode;
import java.util.UUID;

public record AssignRoleCommand(UUID userId, RoleCode role) {
}
