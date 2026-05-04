// Standalone E2E test module — runs against a live environment (docker-compose or staging).
// Execute with: ./gradlew :e2e-tests:test
// Override service URLs via env vars:  AUTH_SERVICE_URL, IDENTITY_SERVICE_URL, etc.
//
// Inherits from root: Java 21 toolchain, spring-boot-starter-test (JUnit 5 + AssertJ),
// Spring Boot BOM. Only REST Assured is added here.

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:json-path:5.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeTags("e2e")
    }

    // Forward env-var overrides as JVM system properties so tests can read them
    // with System.getProperty(). Defaults point to localhost docker-compose ports.
    mapOf(
        "auth.service.url"       to (System.getenv("AUTH_SERVICE_URL")       ?: "http://localhost:8180"),
        "identity.service.url"   to (System.getenv("IDENTITY_SERVICE_URL")   ?: "http://localhost:8083"),
        "form.service.url"       to (System.getenv("FORM_SERVICE_URL")       ?: "http://localhost:8086"),
        "promotion.service.url"  to (System.getenv("PROMOTION_SERVICE_URL")  ?: "http://localhost:8088"),
        "dashboard.service.url"  to (System.getenv("DASHBOARD_SERVICE_URL")  ?: "http://localhost:8084"),
        "e2e.user"               to (System.getenv("E2E_USER")               ?: "staff_guard"),
        "e2e.pass"               to (System.getenv("E2E_PASS")               ?: "password"),
        "e2e.admin.user"         to (System.getenv("E2E_ADMIN_USER")         ?: "health_user"),
        "e2e.admin.pass"         to (System.getenv("E2E_ADMIN_PASS")         ?: "password")
    ).forEach { (key, value) -> systemProperty(key, value) }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
