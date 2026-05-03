---
description: How to run the CircleGuard project locally
---

This workflow guides you through launching the full CircleGuard ecosystem, including infrastructure, microservices, and the mobile frontend.

### 1. Infrastructure Setup (Docker)
Ensure Docker Desktop is running on Windows and integrated with WSL.

// turbo
1. Navigate to the monorepo root:
   `cd /home/toor/git/circleguard`

// turbo
2. Start the infrastructure (PostgreSQL, Neo4j, Kafka, Redis, LDAP):
   `docker-compose -f docker-compose.dev.yml up -d`

### 2. Backend Services (Spring Boot)
Ensure the Gradle wrapper has executable permissions:
`chmod +x gradlew`

In a monorepo, you must specify which service to run. Launch these in separate terminals:

// turbo
- **Auth Service:** `./gradlew :services:circleguard-auth-service:bootRun`
- **Promotion Service:** `./gradlew :services:circleguard-promotion-service:bootRun`
- **Gateway Service:** `./gradlew :services:circleguard-gateway-service:bootRun`
- **Dashboard Service:** `./gradlew :services:circleguard-dashboard-service:bootRun`

### 3. Mobile Frontend (Expo)
Prepare and launch the React Native application.

1. Navigate to the mobile directory:
   `cd mobile`

2. Install dependencies:
   `npm install`

3. Start the Expo development server:
   `npx expo start`
   *Press `w` to open in web browser or `a`/`i` for Android/iOS simulators.*

### 4. Verification
- **API Gateway:** [http://localhost:8080](http://localhost:8080)
- **Neo4j Browser:** [http://localhost:7474](http://localhost:7474) (Login: `neo4j/password`)
- **Dashboard API:** [http://localhost:8085/api/v1/analytics/trends/{id}](http://localhost:8085/api/v1/analytics/trends/{id})
