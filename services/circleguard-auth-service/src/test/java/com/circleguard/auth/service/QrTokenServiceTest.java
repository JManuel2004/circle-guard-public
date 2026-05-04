package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QrTokenServiceTest {

    private static final String SECRET     = "my-qr-secret-key-for-dev-1234567890";
    private static final long   EXPIRY_MS  = 300_000L; // 300 s

    private QrTokenService service;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        service    = new QrTokenService(SECRET, EXPIRY_MS);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // The subject must carry the anonymousId, never the real identity
    @Test
    void generateQrToken_SubjectEqualsAnonymousId() {
        UUID id = UUID.randomUUID();

        Claims claims = parseClaims(service.generateQrToken(id));

        assertThat(claims.getSubject()).isEqualTo(id.toString());
    }

    // Token must expire within the configured window (300 s from issuance)
    @Test
    void generateQrToken_ExpiresWithinConfiguredWindow() {
        long before = System.currentTimeMillis();
        Claims claims = parseClaims(service.generateQrToken(UUID.randomUUID()));
        long after  = System.currentTimeMillis();

        long expMs = claims.getExpiration().getTime();
        // JWT stores `exp` as whole seconds, so getTime() can be up to 999 ms below
        // the intended expiry (truncation, not rounding). Allow a 1-second slack on
        // the lower bound; add the same slack on the upper bound for execution time.
        assertThat(expMs).isBetween(before + EXPIRY_MS - 1_000L, after + EXPIRY_MS + 1_000L);
    }

    // Tokens for different users must differ (prevents token reuse)
    @Test
    void generateQrToken_DifferentIds_ProduceDifferentTokens() {
        String tokenA = service.generateQrToken(UUID.randomUUID());
        String tokenB = service.generateQrToken(UUID.randomUUID());

        assertThat(tokenA).isNotEqualTo(tokenB);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
