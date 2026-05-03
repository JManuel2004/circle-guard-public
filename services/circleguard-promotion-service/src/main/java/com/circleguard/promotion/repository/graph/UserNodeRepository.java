package com.circleguard.promotion.repository.graph;

import com.circleguard.promotion.model.graph.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, String> {
    
    @Query("MATCH (u1:User {anonymousId: $sourceId}), (u2:User {anonymousId: $targetId}) " +
           "MERGE (u1)-[r:ENCOUNTERED {locationId: $locationId}]-(u2) " +
           "ON CREATE SET r.startTime = $timestamp, r.duration = 0 " +
           "ON MATCH SET r.duration = ($timestamp - r.startTime) / 1000")
    void recordEncounter(String sourceId, String targetId, Long timestamp, String locationId);

    @Query("MATCH ()-[r]-() WHERE id(r) = $relId SET r.isValid = NOT coalesce(r.isValid, true)")
    void toggleEncounterValidity(Long relId);

    @Query("MATCH (u1:User)-[r:ENCOUNTERED]-(u2:User) WHERE id(r) = $relId SET r.forceFence = true")
    void forceEncounterFence(Long relId);

    @Query("MATCH ()-[r:ENCOUNTERED]-() WHERE r.startTime < $threshold DELETE r RETURN count(r)")
    Long purgeStaleEncounters(Long threshold);

    @Query("MATCH (u1:User {anonymousId: $a1})-[r:ENCOUNTERED]-(u2:User {anonymousId: $a2}) RETURN sum(r.duration)")
    Long getCumulativeEncounterDuration(String a1, String a2);

    @Query("MATCH (u1:User {anonymousId: $id})-[:MEMBER_OF]->(c:Circle)<-[:MEMBER_OF]-(u2:User) " +
           "WHERE u1 <> u2 RETURN count(DISTINCT u2)")
    Long getConfirmedConnectionCount(String id);

    @Query("MATCH (u1:User {anonymousId: $id})-[r:ENCOUNTERED]-(u2:User) " +
           "WHERE NOT (u1)-[:MEMBER_OF]->(:Circle)<-[:MEMBER_OF]-(u2) " +
           "AND u1 <> u2 " +
           "RETURN count(DISTINCT u2)")
    Long getUnconfirmedConnectionCount(String id);
}
