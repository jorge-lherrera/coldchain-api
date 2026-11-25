package com.coldchain.modules.identity.internal.infrastructure.security;

import com.coldchain.modules.identity.internal.domain.model.PlatformClient;
import com.coldchain.modules.identity.internal.domain.service.PlatformClientDirectory;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredPlatformClientDirectory implements PlatformClientDirectory {

    private final PlatformClientProperties properties;

    public ConfiguredPlatformClientDirectory(PlatformClientProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<PlatformClient> find(String clientId) {
        if (!properties.configured() || !properties.clientId().equals(clientId)) {
            return Optional.empty();
        }
        return Optional.of(new PlatformClient(properties.clientId(), properties.clientSecretHash()));
    }
}
