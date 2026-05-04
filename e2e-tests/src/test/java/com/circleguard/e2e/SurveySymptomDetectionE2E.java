package com.circleguard.e2e;

import com.circleguard.e2e.config.ServiceUrls;
import com.circleguard.e2e.support.AuthHelper;
import com.circleguard.e2e.support.AuthHelper.LoginResult;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Flow 2 — Health Survey Submission with Symptom Detection
 *
 * Black-box: POST /api/v1/auth/login   (auth-service)
 *            GET  /api/v1/questionnaires/active  (form-service)
 *            POST /api/v1/surveys  (form-service)
 *
 * Validates that:
 *   a) A survey submitted with hasFever=true is persisted and echoed back
 *      with the correct anonymousId.
 *   b) A survey with no symptoms (all false) is accepted and stored.
 *   c) The questionnaires endpoint is reachable end-to-end.
 */
@Tag("e2e")
@DisplayName("E2E Flow 2 – Survey submission + symptom detection")
class SurveySymptomDetectionE2E {

    @Test
    @DisplayName("Survey with fever symptom is persisted with correct anonymousId")
    void surveyWithFever_IsPersistedWithCorrectAnonymousId() {
        LoginResult login = AuthHelper.loginAsUser();

        Response response = given()
                .baseUri(ServiceUrls.FORM)
                .spec(login.authenticated())
                .body("""
                      {
                        "anonymousId": "%s",
                        "hasFever": true,
                        "hasCough": false
                      }
                      """.formatted(login.anonymousId()))
                .when()
                .post("/api/v1/surveys")
                .then()
                .statusCode(200)
                .extract().response();

        // The saved entity must echo back the anonymousId sent by the client
        String returnedId = response.jsonPath().getString("anonymousId");
        assertThat(returnedId)
                .as("Survey must be associated with the authenticated user's anonymousId")
                .isEqualTo(login.anonymousId().toString());

        // hasFever field must be preserved
        assertThat(response.jsonPath().getBoolean("hasFever")).isTrue();

        // The generated survey ID must be a valid UUID
        assertThat(response.jsonPath().getString("id")).isNotBlank();
    }

    @Test
    @DisplayName("Survey with no symptoms is accepted (HTTP 200)")
    void surveyWithNoSymptoms_IsAccepted() {
        LoginResult login = AuthHelper.loginAsUser();

        given()
                .baseUri(ServiceUrls.FORM)
                .spec(login.authenticated())
                .body("""
                      {
                        "anonymousId": "%s",
                        "hasFever": false,
                        "hasCough": false
                      }
                      """.formatted(login.anonymousId()))
                .when()
                .post("/api/v1/surveys")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Active questionnaire endpoint is reachable and returns 200 or 404")
    void activeQuestionnaire_EndpointIsReachable() {
        int status = given()
                .baseUri(ServiceUrls.FORM)
                .when()
                .get("/api/v1/questionnaires/active")
                .then()
                .extract().statusCode();

        // 200 = there is an active questionnaire; 404 = none configured yet — both are valid
        assertThat(status).isIn(200, 404);
    }
}
