package com.safipay.stokvel.security;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key; import java.util.Date;

@Component @Slf4j
public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }
    public String extractUserId(String t) { return claims(t).getSubject(); }
    public boolean validate(String t) {
        try { return !claims(t).getExpiration().before(new Date()); }
        catch (Exception e) { return false; }
    }
    private Claims claims(String t) { return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(t).getBody(); }
}
