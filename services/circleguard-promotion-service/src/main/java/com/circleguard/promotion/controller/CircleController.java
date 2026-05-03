package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.service.CircleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/circles")
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circleService;

    @Data
    public static class CircleCreateRequest {
        private String name;
        private String locationId;
    }

    @PostMapping
    public ResponseEntity<CircleNode> createCircle(@RequestBody CircleCreateRequest request) {
        return ResponseEntity.ok(circleService.createCircle(request.getName(), request.getLocationId()));
    }

    @PostMapping("/join/{code}/user/{anonymousId}")
    public ResponseEntity<CircleNode> joinCircle(@PathVariable String code, @PathVariable String anonymousId) {
        return ResponseEntity.ok(circleService.joinCircle(anonymousId, code));
    }

    @PostMapping("/{id}/members/{anonymousId}")
    public ResponseEntity<CircleNode> addMember(@PathVariable Long id, @PathVariable String anonymousId) {
        return ResponseEntity.ok(circleService.addMember(id, anonymousId));
    }

    @GetMapping("/user/{anonymousId}")
    public ResponseEntity<List<CircleNode>> getUserCircles(@PathVariable String anonymousId) {
        return ResponseEntity.ok(circleService.getUserCircles(anonymousId));
    }

    @PatchMapping("/{id}/validity")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> toggleValidity(@PathVariable Long id) {
        circleService.toggleCircleValidity(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/force-fence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> forceFence(@PathVariable Long id) {
        circleService.forceFenceCircle(id);
        return ResponseEntity.ok().build();
    }
}
