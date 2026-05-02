package com.safipay.user.security;

import com.safipay.user.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component @Slf4j
public class JwtUtil {

    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long expiration;
    @Value("${jwt.refresh-expiration}") private long refreshExpiration;

    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateAccessToken(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("fullName", user.getFullName());
        return buildToken(claims, user.getId(), expiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getId(), refreshExpiration);
    }

    private String buildToken(Map<String,Object> claims, String subject, long exp) {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + exp))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUserId(String token) { return extractClaims(token).getSubject(); }
    public String extractEmail(String token) { return (String) extractClaims(token).get("email"); }

    public boolean validateToken(String token) {
        try { extractClaims(token); return true; }
        catch (Exception e) { log.warn("Invalid JWT: {}", e.getMessage()); return false; }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }
}
