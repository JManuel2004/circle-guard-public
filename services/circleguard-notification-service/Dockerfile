FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY services/ services/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-notification-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/services/circleguard-notification-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
