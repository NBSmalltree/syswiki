package com.syswiki.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${syswiki.jwt.secret}")
    private String secret;

    @Value("${syswiki.jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT_SECRET 环境变量未设置！请在启动前配置该变量。"
                + " 示例：export JWT_SECRET=your-secret-key-at-least-32-bytes");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET 长度不足！当前 " + keyBytes.length + " 字节，需要至少 32 字节。"
                + " 生成方法：openssl rand -hex 32");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String username, String role) {
        return Jwts.builder()
            .setSubject(userId)
            .claim("username", username)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) { return parseClaims(token).getSubject(); }
    public String getUsername(String token) { return parseClaims(token).get("username", String.class); }
    public String getRole(String token) { return parseClaims(token).get("role", String.class); }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
