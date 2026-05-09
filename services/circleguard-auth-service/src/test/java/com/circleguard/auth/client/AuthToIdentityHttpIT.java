package com.circleguard.auth.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: validates the HTTP contract between auth-service's IdentityClient
 * and identity-service's POST /api/v1/identities/map endpoint.
 *
 * Uses WireMock on port 8083 (IdentityClient's hardcoded target) to stand in for
 * the real identity-service, verifying that the client sends the correct payload
 * and correctly deserializes the anonymousId from the response.
 */
class AuthToIdentityHttpIT {

    // Binds to the same port that IdentityClient has hardcoded
    @RegisterExtension
    static WireMockExtension identityServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8083))
            .build();

    private IdentityClient client;

    @BeforeEach
    void setUp() {
        client = new IdentityClient("http://localhost:" + identityServiceMock.getPort());
    }

    @Test
    void getAnonymousId_SendsRealIdentityAndParsesAnonymousIdFromResponse() {
        UUID expectedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String realIdentity = "student@university.edu";

        identityServiceMock.stubFor(post(urlEqualTo("/api/v1/identities/map"))
                .withRequestBody(containing("\"realIdentity\":\"" + realIdentity + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"anonymousId\":\"" + expectedId + "\"}")));

        UUID result = client.getAnonymousId(realIdentity);

        assertThat(result).isEqualTo(expectedId);
        identityServiceMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/identities/map")));
    }

    @Test
    void getAnonymousId_AlwaysSendsPostNotGet() {
        UUID anyId = UUID.randomUUID();

        identityServiceMock.stubFor(post(urlEqualTo("/api/v1/identities/map"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"anonymousId\":\"" + anyId + "\"}")));

        client.getAnonymousId("someone@uni.edu");

        // Must never have issued a GET — identity mapping is always a write-or-lookup
        identityServiceMock.verify(0, getRequestedFor(anyUrl()));
        identityServiceMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/identities/map")));
    }
}
