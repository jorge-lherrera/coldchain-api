package com.coldchain.modules.identity.internal.application;

import com.coldchain.modules.identity.api.event.AccessRefreshed;
import com.coldchain.modules.identity.api.event.ApiClientCreated;
import com.coldchain.modules.identity.api.event.ClientTokenIssued;
import com.coldchain.modules.identity.api.event.IdentityEvent;
import com.coldchain.modules.identity.api.event.OrganizationRegistered;
import com.coldchain.modules.identity.api.event.RefreshTokenReuseDetected;
import com.coldchain.modules.identity.api.event.RoleAssigned;
import com.coldchain.modules.identity.api.event.RoleRevoked;
import com.coldchain.modules.identity.api.event.UserActivated;
import com.coldchain.modules.identity.api.event.UserAuthenticated;
import com.coldchain.modules.identity.api.event.UserInvited;
import com.coldchain.modules.identity.internal.domain.model.AuditEntry;
import com.coldchain.modules.identity.internal.domain.repository.AuditEntryRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class IdentityAuditListener {

    private final AuditEntryRepository auditEntries;

    private final ObjectMapper json;

    public IdentityAuditListener(AuditEntryRepository auditEntries, ObjectMapper json) {
        this.auditEntries = auditEntries;
        this.json = json;
    }

    @EventListener
    public void on(IdentityEvent event) {
        AuditedFacts facts = factsOf(event);
        auditEntries.save(AuditEntry.record(event.organizationId(), event.actorType(), event.actorId(),
                facts.action(), facts.resourceType(), facts.resourceId(), serialise(facts.payload()),
                event.occurredAt()));
    }

    private String serialise(Map<String, Object> payload) {
        return payload == null ? null : json.writeValueAsString(payload);
    }

    private static AuditedFacts factsOf(IdentityEvent event) {
        return switch (event) {
            case OrganizationRegistered fact -> new AuditedFacts("ORGANIZATION_REGISTERED", "ORGANIZATION",
                    fact.organizationId(), Map.of("taxId", fact.taxId()));
            case UserInvited fact -> new AuditedFacts("USER_INVITED", "USER", fact.invitedUserId(),
                    Map.of("role", fact.role()));
            case UserActivated fact -> new AuditedFacts("USER_ACTIVATED", "USER", fact.actorId(), null);
            case UserAuthenticated fact -> new AuditedFacts("USER_AUTHENTICATED", "USER", fact.actorId(),
                    null);
            case AccessRefreshed fact -> new AuditedFacts("ACCESS_REFRESHED", "REFRESH_TOKEN",
                    fact.familyId(), null);
            case RefreshTokenReuseDetected fact -> new AuditedFacts("REFRESH_TOKEN_REUSE_DETECTED",
                    "REFRESH_TOKEN", fact.familyId(), Map.of("revokedTokens", fact.revokedTokens()));
            case ClientTokenIssued fact -> new AuditedFacts("CLIENT_TOKEN_ISSUED", "API_CLIENT",
                    fact.actorId(), null);
            case ApiClientCreated fact -> new AuditedFacts("API_CLIENT_CREATED", "API_CLIENT",
                    fact.apiClientId(), Map.of("label", fact.label(), "scopes", fact.scopes()));
            case RoleAssigned fact -> new AuditedFacts("ROLE_ASSIGNED", "USER", fact.subjectId(),
                    Map.of("role", fact.role()));
            case RoleRevoked fact -> new AuditedFacts("ROLE_REVOKED", "USER", fact.subjectId(),
                    Map.of("role", fact.role()));
        };
    }

    private record AuditedFacts(String action, String resourceType, UUID resourceId,
            Map<String, Object> payload) {
    }
}
