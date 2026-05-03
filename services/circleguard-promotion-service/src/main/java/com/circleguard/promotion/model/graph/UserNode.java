package com.circleguard.promotion.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;
import java.util.*;

@Node("User")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNode {
    @Id
    private String anonymousId;

    @Property("status")
    private String status; // ACTIVE, CONTAGIED, RECOVERED, etc.

    @Property("statusUpdatedAt")
    private Long statusUpdatedAt;

    @Relationship(type = "ENCOUNTERED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<EncounterRelationship> encounters = new HashSet<>();
}
