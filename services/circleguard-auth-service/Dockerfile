FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY services/ services/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-auth-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/services/circleguard-auth-service/build/libs/*.jar app.jar

EXPOSE 8180

ENTRYPOINT ["java", "-jar", "app.jar"]
