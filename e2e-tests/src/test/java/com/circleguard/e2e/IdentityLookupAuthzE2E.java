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
 * E2E Flow 3 — Permission-gated Identity Lookup
 *
 * Black-box: POST /api/v1/auth/login                    (auth-service)
 *            POST /api/v1/identities/map                (identity-service)
 *            GET  /api/v1/identities/lookup/{id}        (identity-service)
 *
 * Validates that:
 *   a) A regular user (without identity:lookup) receives 403 Forbidden on lookup.
 *   b) An admin with identity:lookup permission receives 200 OK on the same endpoint.
 *   c) The lookup response contains the original real identity (round-trip integrity).
 */
@Tag("e2e")
@DisplayName("E2E Flow 3 – Permission-gated identity lookup (auth → identity)")
class IdentityLookupAuthzE2E {

    private static final String TARGET_IDENTITY = "lookup-target@university.edu";

    /** Creates the mapping and returns its anonymousId. */
    private UUID ensureMapping(String realIdentity) {
        return given()
                .baseUri(ServiceUrls.IDENTITY)
                .contentType(ContentType.JSON)
                .body("""
                      {"realIdentity": "%s"}
                      """.formatted(realIdentity))
                .when()
                .post("/api/v1/identities/map")
                .then()
                .statusCode(200)
                .extract().jsonPath().getUUID("anonymousId");
    }

    @Test
    @DisplayName("Regular user without identity:lookup gets 401 or 403 on lookup endpoint")
    void regularUser_CannotLookUpIdentity() {
        UUID anonymousId = ensureMapping(TARGET_IDENTITY);
        LoginResult user = AuthHelper.loginAsUser();

        int status = given()
                .baseUri(ServiceUrls.IDENTITY)
                .spec(user.authenticated())
                .when()
                .get("/api/v1/identities/lookup/" + anonymousId)
                .then()
                .extract().statusCode();

        // 401 = JWT not carrying the required authority; 403 = authority missing
        assertThat(status).as("Low-privilege user must be denied lookup").isIn(401, 403);
    }

    @Test
    @DisplayName("Admin with identity:lookup gets 200 and the correct real identity")
    void adminWithPermission_CanLookUpIdentity() {
        UUID anonymousId = ensureMapping(TARGET_IDENTITY);
        LoginResult admin = AuthHelper.loginAsAdmin();

        String resolvedIdentity = given()
                .baseUri(ServiceUrls.IDENTITY)
                .spec(admin.authenticated())
                .when()
                .get("/api/v1/identities/lookup/" + anonymousId)
                .then()
                .statusCode(200)
                .extract().asString();

        // The decrypted real identity must survive the full round-trip
        assertThat(resolvedIdentity).contains(TARGET_IDENTITY);
    }

    @Test
    @DisplayName("Unauthenticated request to lookup is rejected with 401 or 403")
    void unauthenticated_CannotLookUpIdentity() {
        UUID anonymousId = ensureMapping("noauth-test@university.edu");

        int status = given()
                .baseUri(ServiceUrls.IDENTITY)
                .when()
                .get("/api/v1/identities/lookup/" + anonymousId)
                .then()
                .extract().statusCode();

        assertThat(status).isIn(401, 403);
    }
}
