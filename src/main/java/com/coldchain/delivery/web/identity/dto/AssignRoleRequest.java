package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.RoleCode;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(@NotNull RoleCode role) {
}
