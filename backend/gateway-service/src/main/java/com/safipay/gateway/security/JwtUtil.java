package com.safipay.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public boolean validate(String token) {
        try {
            return !Jwts.parserBuilder().setSigningKey(key()).build()
                    .parseClaimsJws(token).getBody().getExpiration().before(new Date());
        } catch (Exception e) { return false; }
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
