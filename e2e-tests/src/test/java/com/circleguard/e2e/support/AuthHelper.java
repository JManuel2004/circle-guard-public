package com.circleguard.e2e.support;

import com.circleguard.e2e.config.ServiceUrls;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Utility that authenticates against auth-service and wraps the resulting
 * JWT + anonymousId for use in subsequent REST Assured requests.
 */
public final class AuthHelper {

    private AuthHelper() {}

    public record LoginResult(String token, UUID anonymousId) {
        /** Returns a RequestSpecification with the Bearer token pre-set. */
        public RequestSpecification authenticated() {
            return given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token);
        }
    }

    /** Logs in with explicit credentials. Throws AssertionError on HTTP != 200. */
    public static LoginResult loginAs(String username, String password) {
        Response response = given()
                .baseUri(ServiceUrls.AUTH)
                .contentType(ContentType.JSON)
                .body("""
                      {"username": "%s", "password": "%s"}
                      """.formatted(username, password))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token      = response.jsonPath().getString("token");
        String rawId      = response.jsonPath().getString("anonymousId");
        return new LoginResult(token, UUID.fromString(rawId));
    }

    /** Shorthand: logs in with the default test user. */
    public static LoginResult loginAsUser() {
        return loginAs(ServiceUrls.USER, ServiceUrls.USER_PASS);
    }

    /** Shorthand: logs in with the default admin user. */
    public static LoginResult loginAsAdmin() {
        return loginAs(ServiceUrls.ADMIN, ServiceUrls.ADMIN_PASS);
    }
}
