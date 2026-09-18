package com.coldchain.modules.shipment.api.dto;

public record ChainVerdict(boolean intact, Integer firstBrokenSequence, String reason) {
}
