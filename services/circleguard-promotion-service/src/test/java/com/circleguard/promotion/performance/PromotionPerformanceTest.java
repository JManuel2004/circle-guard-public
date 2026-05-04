package com.circleguard.promotion.performance;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class PromotionPerformanceTest {

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.12")
            .withAdminPassword("password");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2.1")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "password");
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired
    private HealthStatusService healthStatusService;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private Neo4jClient neo4jClient;

    private String rootUser;

    @BeforeEach
    void setupBenchmarkData() {
        // Clear graph
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // Create 10,000 nodes and random contacts
        rootUser = UUID.randomUUID().toString();
        
        // 1. Create root user
        neo4jClient.query("CREATE (:User {anonymousId: $id, status: 'ACTIVE'})")
                .bind(rootUser).to("id").run();

        // 2. Create 10,000 secondary nodes in batches for performance
        // This is a simplified scale model for benchmarking
        neo4jClient.query("UNWIND range(1, 10000) as i " +
                "CREATE (u:User {anonymousId: 'user-' + toString(i), status: 'ACTIVE'})")
                .run();

        // 3. Connect root to a subset (Realistic average)
        neo4jClient.query("MATCH (root:User {anonymousId: $id}), (others:User) " +
                "WHERE others.anonymousId <> $id " +
                "WITH root, others LIMIT 50 " +
                "CREATE (root)-[:ENCOUNTERED {startTime: timestamp()}]->(others)")
                .bind(rootUser).to("id")
                .run();
                
        // Connect others in a chain/mesh (Realistic density)
        neo4jClient.query("MATCH (u1:User), (u2:User) " +
                "WHERE u1.anonymousId <> u2.anonymousId AND rand() < 0.001 " +
                "WITH u1, u2 LIMIT 15000 " +
                "CREATE (u1)-[:ENCOUNTERED {startTime: timestamp()}]->(u2)")
                .run();
    }

    @Test
    void benchmarkPromotionPerformance() {
        System.out.println("Starting Promotion Benchmark...");
        
        // --- Warmup Phase ---
        // Perform a small promotion to warm up indices and JIT
        String warmupUser = "user-1"; 
        healthStatusService.updateStatus(warmupUser, "CONFIRMED");
        System.out.println("Warmup phase complete.");
        
        // --- Main Benchmark ---
        long startTime = System.currentTimeMillis();
        
        // Trigger promotion on rootUser (affects 10,000 node cluster)
        healthStatusService.updateStatus(rootUser, "CONFIRMED");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("==========================================");
        System.out.println("TOTAL DURATION: " + duration + "ms");
        System.out.println("==========================================");
        
        // NFR-1 target in production: < 1000ms.
        // In CI/local with Testcontainers the graph traversal over 10k nodes runs
        // on a cold JVM inside a Docker container, so we allow 10× headroom here.
        assertTrue(duration < 10_000, "Promotion cascade exceeded 10 second CI threshold. Actual: " + duration + "ms");

        // --- Multi-Tier Validation ---
        // Verify L1 promotion (SUSPECT)
        Long suspectCount = neo4jClient.query("MATCH (root:User {anonymousId: $id})-[:ENCOUNTERED]-(c1:User) " +
                "WHERE c1.status = 'SUSPECT' RETURN count(c1) as count")
                .bind(rootUser).to("id")
                .fetchAs(Long.class).one().get();
        System.out.println("L1 SUSPECT COUNT: " + suspectCount);
        assertTrue(suspectCount > 0, "No L1 contacts were promoted to SUSPECT");

        // Verify L2 promotion (PROBABLE)
        Long probableCount = neo4jClient.query("MATCH (root:User {anonymousId: $id})-[:ENCOUNTERED]-(c1)-[:ENCOUNTERED]-(c2:User) " +
                "WHERE c2.status = 'PROBABLE' AND c2.anonymousId <> root.anonymousId RETURN count(c2) as count")
                .bind(rootUser).to("id")
                .fetchAs(Long.class).one().get();
        System.out.println("L2 PROBABLE COUNT: " + probableCount);
        assertTrue(probableCount > 0, "No L2 contacts were promoted to PROBABLE");
    }
}
