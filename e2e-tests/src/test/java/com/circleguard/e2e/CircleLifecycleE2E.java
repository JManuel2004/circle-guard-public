package com.circleguard.e2e;

import com.circleguard.e2e.config.ServiceUrls;
import com.circleguard.e2e.support.AuthHelper;
import com.circleguard.e2e.support.AuthHelper.LoginResult;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Flow 4 — Full Circle Lifecycle (Create → Join → Verify Membership)
 *
 * Black-box: POST /api/v1/auth/login                         (auth-service)
 *            POST /api/v1/circles                             (promotion-service)
 *            POST /api/v1/circles/join/{code}/user/{id}       (promotion-service)
 *            GET  /api/v1/circles/user/{anonymousId}          (promotion-service)
 *
 * Validates that:
 *   a) A circle can be created and returns a valid MESH-XXXX invite code.
 *   b) A user can join the circle using the invite code.
 *   c) After joining, the circle appears in the user's circle list.
 */
@Tag("e2e")
@DisplayName("E2E Flow 4 – Circle lifecycle (auth → promotion)")
class CircleLifecycleE2E {

    @Test
    @DisplayName("Create circle, join it, verify user appears as member")
    void circleLifecycle_CreateJoinAndVerifyMembership() {
        // Step 1: authenticate as a user
        LoginResult user = AuthHelper.loginAsUser();
        String anonymousId = user.anonymousId().toString();

        // Step 2: create a circle (any authenticated user can do this)
        Response createResponse = given()
                .baseUri(ServiceUrls.PROMOTION)
                .spec(user.authenticated())
                .body("""
                      {"name": "E2E Test Circle", "locationId": "room-e2e-001"}
                      """)
                .when()
                .post("/api/v1/circles")
                .then()
                .statusCode(200)
                .extract().response();

        String inviteCode = createResponse.jsonPath().getString("inviteCode");
        assertThat(inviteCode)
                .as("Invite code must match the MESH-XXXX format")
                .matches("MESH-[A-Z2-9]{4}");

        // Step 3: the same user joins the circle using the invite code
        given()
                .baseUri(ServiceUrls.PROMOTION)
                .spec(user.authenticated())
                .when()
                .post("/api/v1/circles/join/{code}/user/{anonymousId}", inviteCode, anonymousId)
                .then()
                .statusCode(200);

        // Step 4: verify the circle appears in the user's circle list
        List<String> circleCodes = given()
                .baseUri(ServiceUrls.PROMOTION)
                .spec(user.authenticated())
                .when()
                .get("/api/v1/circles/user/{anonymousId}", anonymousId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("inviteCode", String.class);

        assertThat(circleCodes)
                .as("User's circle list must contain the circle they just joined")
                .contains(inviteCode);
    }

    @Test
    @DisplayName("Created circle has correct name and location")
    void createCircle_ResponseContainsSubmittedFields() {
        LoginResult user = AuthHelper.loginAsUser();

        Response response = given()
                .baseUri(ServiceUrls.PROMOTION)
                .spec(user.authenticated())
                .body("""
                      {"name": "Metadata Verification Circle", "locationId": "room-meta-002"}
                      """)
                .when()
                .post("/api/v1/circles")
                .then()
                .statusCode(200)
                .extract().response();

        assertThat(response.jsonPath().getString("name")).isEqualTo("Metadata Verification Circle");
        assertThat(response.jsonPath().getString("locationId")).isEqualTo("room-meta-002");
        assertThat(response.jsonPath().getBoolean("isActive")).isTrue();
    }
}
