package com.coldchain.modules.identity.internal.domain.service;

import com.coldchain.modules.identity.internal.domain.model.PlatformClient;
import java.util.Optional;

public interface PlatformClientDirectory {

    Optional<PlatformClient> find(String clientId);
}
