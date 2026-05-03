package com.circleguard.promotion.model.graph;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import lombok.*;

@RelationshipProperties
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncounterRelationship {
    @RelationshipId
    private Long id;

    @TargetNode
    private UserNode target;

    private Long startTime;
    private Long duration; // in seconds
    private String locationId; // The AP or Zone ID where it happened
    @Builder.Default
    private boolean isValid = true;
    @Builder.Default
    private boolean forceFence = false;
}
