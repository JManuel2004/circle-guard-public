package com.circleguard.e2e.config;

/**
 * Reads service base URLs and test credentials from JVM system properties,
 * which are injected by build.gradle.kts from environment variables.
 *
 * In CI/CD set the matching env vars to point at the deployed environment:
 *   AUTH_SERVICE_URL, IDENTITY_SERVICE_URL, FORM_SERVICE_URL,
 *   PROMOTION_SERVICE_URL, DASHBOARD_SERVICE_URL,
 *   E2E_USER, E2E_PASS, E2E_ADMIN_USER, E2E_ADMIN_PASS
 */
public final class ServiceUrls {

    public static final String AUTH      = prop("auth.service.url",      "http://localhost:8180");
    public static final String IDENTITY  = prop("identity.service.url",  "http://localhost:8083");
    public static final String FORM      = prop("form.service.url",      "http://localhost:8086");
    public static final String PROMOTION = prop("promotion.service.url", "http://localhost:8088");
    public static final String DASHBOARD = prop("dashboard.service.url", "http://localhost:8084");

    // Test credentials — must match Flyway seed data in auth-service
    public static final String USER       = prop("e2e.user",       "staff_guard");
    public static final String USER_PASS  = prop("e2e.pass",       "password");
    public static final String ADMIN      = prop("e2e.admin.user", "health_user");
    public static final String ADMIN_PASS = prop("e2e.admin.pass", "password");

    private ServiceUrls() {}

    private static String prop(String key, String fallback) {
        String val = System.getProperty(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
