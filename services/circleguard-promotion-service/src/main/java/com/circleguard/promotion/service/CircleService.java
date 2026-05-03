package com.circleguard.promotion.service;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.repository.graph.CircleNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircleService {

    private final CircleNodeRepository circleRepository;
    private final HealthStatusService healthStatusService;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Transactional("neo4jTransactionManager")
    public void toggleCircleValidity(Long circleId) {
        CircleNode circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new RuntimeException("Circle not found"));
        
        boolean newValid = !circle.getIsValid();
        circle.setIsValid(newValid);
        circleRepository.save(circle);
        
        log.info("Circle {} validity toggled to: {}", circleId, newValid);
        
        if (!newValid) {
            // Trigger Pulse Recovery for all members since their risk path might have cleared
            circle.getMembers().forEach(user -> healthStatusService.resolveStatus(user.getAnonymousId()));
        }
    }

    @Transactional("neo4jTransactionManager")
    public void forceFenceCircle(Long circleId) {
        CircleNode circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new RuntimeException("Circle not found"));
        
        circle.setForceFence(true);
        circleRepository.save(circle);
        
        log.info("Force fencing initiated for circle: {}", circle.getName());
        
        // Promote all ACTIVE members to PROBABLE
        circle.getMembers().forEach(user -> {
            if ("ACTIVE".equals(user.getStatus())) {
                healthStatusService.updateStatus(user.getAnonymousId(), "PROBABLE");
            }
        });
    }

    @Transactional("neo4jTransactionManager")
    public CircleNode createCircle(String name, String locationId) {
        String inviteCode = generateUniqueInviteCode();
        CircleNode circle = CircleNode.builder()
                .name(name)
                .inviteCode(inviteCode)
                .createdAt(System.currentTimeMillis())
                .locationId(locationId)
                .isActive(true)
                .build();
        
        CircleNode saved = circleRepository.save(circle);
        log.info("Created Circle: {} (Code: {})", name, inviteCode);
        return saved;
    }

    @Transactional("neo4jTransactionManager")
    public CircleNode joinCircle(String anonymousId, String inviteCode) {
        log.info("User {} attempting to join circle {}", anonymousId, inviteCode);
        return circleRepository.joinCircle(anonymousId, inviteCode)
                .orElseThrow(() -> new RuntimeException("Invalid invite code or user already member"));
    }

    @Transactional("neo4jTransactionManager")
    public CircleNode addMember(Long circleId, String anonymousId) {
        log.info("Manually adding user {} to circle {}", anonymousId, circleId);
        return circleRepository.addUserToCircle(anonymousId, circleId)
                .orElseThrow(() -> new RuntimeException("Failed to add user to circle"));
    }

    public List<CircleNode> getUserCircles(String anonymousId) {
        return circleRepository.findCirclesByUser(anonymousId);
    }

    private String generateUniqueInviteCode() {
        Random random = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            sb.append("MESH-");
            for (int i = 0; i < 4; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (circleRepository.existsByInviteCode(code));
        return code;
    }
}
