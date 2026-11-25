package com.coldchain.modules.shipment.api.dto;

import com.coldchain.modules.shipment.api.Participation;
import java.time.Instant;

public record CustodyChange(Instant occurredAt, Participation from, Participation to) {
}
