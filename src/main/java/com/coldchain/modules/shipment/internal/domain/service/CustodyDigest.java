package com.coldchain.modules.shipment.internal.domain.service;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public final class CustodyDigest {

    private CustodyDigest() {
    }

    public static String of(int sequenceNumber, CustodyEventKind kind, UUID fromOrganizationId,
            UUID toOrganizationId, UUID siteId, UUID actorId, Instant occurredAt, String previousHash) {
        String material = String.join("|",
                Integer.toString(sequenceNumber),
                kind.name(),
                text(fromOrganizationId),
                text(toOrganizationId),
                text(siteId),
                text(actorId),
                occurredAt.toString(),
                previousHash);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", cause);
        }
    }

    private static String text(UUID value) {
        return value == null ? "-" : value.toString();
    }
}
