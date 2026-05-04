package com.circleguard.e2e;

import com.circleguard.e2e.config.ServiceUrls;
import com.circleguard.e2e.support.AuthHelper;
import com.circleguard.e2e.support.AuthHelper.LoginResult;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Flow 1 — Authentication + Persistent Identity Mapping
 *
 * Black-box: POST /api/v1/auth/login  (auth-service)
 *            POST /api/v1/identities/map  (identity-service)
 *
 * Validates that:
 *   a) login returns a well-formed JWT + anonymousId
 *   b) the anonymousId in the JWT matches what identity-service returns for the same
 *      username — proving auth → identity wiring is deterministic end-to-end.
 */
@Tag("e2e")
@DisplayName("E2E Flow 1 – Auth + Identity pairing")
class AuthIdentityPairingE2E {

    @Test
    @DisplayName("Login returns JWT with valid anonymousId")
    void login_ReturnsJwtAndAnonymousId() {
        LoginResult result = AuthHelper.loginAsUser();

        assertThat(result.token()).isNotBlank();
        assertThat(result.anonymousId()).isNotNull();
    }

    @Test
    @DisplayName("anonymousId from JWT matches identity-service mapping for same username")
    void anonymousIdFromLogin_MatchesIdentityServiceMapping() {
        // Step 1: authenticate — auth-service calls identity-service internally
        LoginResult login = AuthHelper.loginAsUser();

        // Step 2: call identity-service directly with the same real identity
        UUID mappedId = given()
                .baseUri(ServiceUrls.IDENTITY)
                .contentType(ContentType.JSON)
                .body("""
                      {"realIdentity": "%s"}
                      """.formatted(ServiceUrls.USER))
                .when()
                .post("/api/v1/identities/map")
                .then()
                .statusCode(200)
                .extract().jsonPath().getUUID("anonymousId");

        // The mapping must be deterministic: same identity → same UUID on every call
        assertThat(login.anonymousId())
                .as("anonymousId in JWT must equal the one stored in identity-service")
                .isEqualTo(mappedId);
    }

    @Test
    @DisplayName("Identity mapping is idempotent — calling twice returns same UUID")
    void identityMapping_IsIdempotent() {
        UUID firstCall = given()
                .baseUri(ServiceUrls.IDENTITY)
                .contentType(ContentType.JSON)
                .body("""
                      {"realIdentity": "idempotency-check@university.edu"}
                      """)
                .when().post("/api/v1/identities/map")
                .then().statusCode(200)
                .extract().jsonPath().getUUID("anonymousId");

        UUID secondCall = given()
                .baseUri(ServiceUrls.IDENTITY)
                .contentType(ContentType.JSON)
                .body("""
                      {"realIdentity": "idempotency-check@university.edu"}
                      """)
                .when().post("/api/v1/identities/map")
                .then().statusCode(200)
                .extract().jsonPath().getUUID("anonymousId");

        assertThat(firstCall).isEqualTo(secondCall);
    }
}
