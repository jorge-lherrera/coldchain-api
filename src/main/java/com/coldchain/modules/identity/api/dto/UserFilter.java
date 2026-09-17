package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.UserStatus;

public record UserFilter(UserStatus status) {

    public static UserFilter none() {
        return new UserFilter(null);
    }
}
