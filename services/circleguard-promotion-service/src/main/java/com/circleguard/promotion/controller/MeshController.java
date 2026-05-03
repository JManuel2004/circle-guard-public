package com.circleguard.promotion.controller;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mesh")
@RequiredArgsConstructor
@Slf4j
public class MeshController {

    private final UserNodeRepository userRepository;

    @Data
    @Builder
    public static class MeshStatsResponse {
        private long confirmedCount;
        private long unconfirmedCount;
    }

    @GetMapping("/stats/{anonymousId}")
    public ResponseEntity<MeshStatsResponse> getMeshStats(@PathVariable String anonymousId) {
        log.info("Fetching mesh visualization stats for user: {}", anonymousId);
        
        long confirmed = userRepository.getConfirmedConnectionCount(anonymousId);
        long unconfirmed = userRepository.getUnconfirmedConnectionCount(anonymousId);
        
        return ResponseEntity.ok(MeshStatsResponse.builder()
                .confirmedCount(confirmed)
                .unconfirmedCount(unconfirmed)
                .build());
    }
}
