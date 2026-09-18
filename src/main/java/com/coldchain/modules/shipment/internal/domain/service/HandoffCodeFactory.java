package com.coldchain.modules.shipment.internal.domain.service;

import java.time.Duration;

public interface HandoffCodeFactory {

    IssuedCode issue();

    String fingerprint(String plainCode);

    Duration lifetime();
}
