package com.coldchain.modules.identity.internal.domain.service;

import java.time.Duration;

public interface OpaqueTokenFactory {

    IssuedSecret issue();

    String fingerprint(String plainToken);

    Duration activationLifetime();
}
