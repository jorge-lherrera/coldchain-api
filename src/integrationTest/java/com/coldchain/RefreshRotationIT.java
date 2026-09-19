package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.AuthenticateCommand;
import com.coldchain.modules.identity.api.dto.RefreshAccessCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.shared.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class RefreshRotationIT {

    private static final String PASSWORD = "Integration!Secret42";

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void rotatingIssuesANewTokenAndSpendsThePresentedOne() {
        TokenResult first = logIn(register());

        TokenResult rotated = identity.refreshAccess(new RefreshAccessCommand(first.refreshToken()));

        assertThat(rotated.refreshToken())
                .describedAs("a refresh that returns the same token is not a rotation")
                .isNotEqualTo(first.refreshToken());
        assertThat(rotated.expiresInSeconds()).isEqualTo(first.expiresInSeconds());
    }

    @Test
    void presentingASpentTokenRevokesTheWholeFamily() {
        TokenResult first = logIn(register());
        TokenResult rotated = identity.refreshAccess(new RefreshAccessCommand(first.refreshToken()));

        assertThatThrownBy(() -> identity.refreshAccess(new RefreshAccessCommand(first.refreshToken())))
                .describedAs("using a spent token is evidence it leaked, not a mistake to forgive")
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("family");

        assertThatThrownBy(() -> identity.refreshAccess(new RefreshAccessCommand(rotated.refreshToken())))
                .describedAs("the token that was still good dies with its family")
                .isInstanceOf(DomainException.class);
    }

    @Test
    void theRevokedFamilyIsRecordedInTheDatabase() {
        TokenResult first = logIn(register());
        identity.refreshAccess(new RefreshAccessCommand(first.refreshToken()));
        try {
            identity.refreshAccess(new RefreshAccessCommand(first.refreshToken()));
        } catch (DomainException expected) {
            assertThat(expected.errorCode().name()).isEqualTo("REFRESH_TOKEN_REUSED");
        }

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM refresh_token WHERE revoked_at IS NULL
                """, Integer.class))
                .describedAs("the revocation survives the rollback that rejecting the caller triggers")
                .isZero();
    }

    private RegisterOrganizationResult register() {
        jdbc.update("DELETE FROM refresh_token");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, "Rotation Labs S.A.", "Rotation", OrganizationKind.SHIPPER, "UY",
                "admin-" + suffix + "@rotation.test", "Rotation Admin", PASSWORD));
    }

    private TokenResult logIn(RegisterOrganizationResult registered) {
        return identity.authenticate(new AuthenticateCommand(registered.email(), PASSWORD));
    }
}
