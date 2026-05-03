package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.MacSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionHandshakeController {
    private final MacSessionRegistry sessionRegistry;

    @PostMapping("/handshake")
    public ResponseEntity<Void> handshake(@RequestBody Map<String, String> request) {
        String macAddress = request.get("macAddress");
        String anonymousId = request.get("anonymousId");
        
        if (macAddress == null || anonymousId == null) {
            return ResponseEntity.badRequest().build();
        }

        sessionRegistry.registerSession(macAddress, anonymousId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{macAddress}")
    public ResponseEntity<Void> closeSession(@PathVariable String macAddress) {
        sessionRegistry.closeSession(macAddress);
        return ResponseEntity.noContent().build();
    }
}
