package com.hospitality.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET_BASE64 = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtUtils jwtUtils;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET_BASE64);
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_BASE64);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(String subject, String email, String username, List<String> roles, long expirationOffsetMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationOffsetMs);
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Should successfully validate token and extract all claims")
    void validateTokenAndExtractClaims_Success() {
        String token = buildToken("99", "admin@hospitality.com", "admin_vikram", List.of("ROLE_ADMIN"), 3600000);

        assertTrue(jwtUtils.validateToken(token));
        assertEquals(99L, jwtUtils.getUserId(token));
        assertEquals("admin@hospitality.com", jwtUtils.getEmail(token));
        assertEquals("admin_vikram", jwtUtils.getUsername(token));
        assertEquals(List.of("ROLE_ADMIN"), jwtUtils.getRoles(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateToken_Expired_ReturnsFalse() {
        // Expiration in the past (-10 seconds)
        String expiredToken = buildToken("99", "admin@hospitality.com", "admin_vikram", List.of("ROLE_ADMIN"), -10000);

        assertFalse(jwtUtils.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject tampered / invalid token")
    void validateToken_Tampered_ReturnsFalse() {
        String validToken = buildToken("99", "admin@hospitality.com", "admin_vikram", List.of("ROLE_ADMIN"), 3600000);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        assertFalse(jwtUtils.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject malformed or blank token")
    void validateToken_Malformed_ReturnsFalse() {
        assertFalse(jwtUtils.validateToken("not-a-valid-jwt"));
        assertFalse(jwtUtils.validateToken(""));
    }
}
