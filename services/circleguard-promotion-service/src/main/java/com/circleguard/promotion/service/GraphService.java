package com.circleguard.promotion.service;

import com.circleguard.promotion.model.graph.*;
import com.circleguard.promotion.repository.graph.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphService {
    private final UserNodeRepository userNodeRepository;
    private final Neo4jClient neo4jClient;

    /**
     * Records an encounter between two users and updates the graph.
     */
    @Transactional
    public void recordEncounter(String userA, String userB, String locationId) {
        userNodeRepository.recordEncounter(userA, userB, System.currentTimeMillis(), locationId);
    }

    /**
     * Detects if a cluster of users in a location should form a new Circle.
     * Logic: If 3+ users are in the same location for > 5 mins.
     */
    @Transactional
    public void detectAndFormCircles(String locationId) {
        // High-performance Cypher query for cluster detection
        String query = "MATCH (u:User)-[r:ENCOUNTERED {locationId: $loc}]->(target:User) " +
                        "WHERE r.duration > 300 " +
                       "WITH collect(DISTINCT u.anonymousId) + collect(DISTINCT target.anonymousId) as allUsers " +
                       "UNWIND allUsers as uid " +
                       "WITH DISTINCT uid " +
                       "WITH collect(uid) as users " +
                       "WHERE size(users) >= 3 " +
                       "UNWIND users as uid " +
                       "MERGE (c:Circle {locationId: $loc, isActive: true}) " +
                       "SET c.name = 'Auto-Circle-' + $loc, c.createdAt = timestamp() " +
                       "WITH c, uid " +
                       "MATCH (u:User {anonymousId: uid}) " +
                       "MERGE (u)-[:MEMBER_OF]->(c)";
        
        neo4jClient.query(query)
                .bind(locationId).to("loc")
                .run();
    }
}
