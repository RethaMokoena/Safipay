package com.safipay.wallet.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component @Slf4j
public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }
    public String extractUserId(String token) { return claims(token).getSubject(); }
    public boolean validate(String token) {
        try { return !claims(token).getExpiration().before(new Date()); }
        catch (Exception e) { log.warn("JWT invalid: {}", e.getMessage()); return false; }
    }
    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }
}
