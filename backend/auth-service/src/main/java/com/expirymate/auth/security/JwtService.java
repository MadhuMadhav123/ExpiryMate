package com.expirymate.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

	private static final long TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000L;
	private final SecretKey key;

	public JwtService(@Value("${jwt.secret}") String secret) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generate(Long userId, String name, String email) {
		long now = System.currentTimeMillis();
		return Jwts.builder().subject(email).claim("userId", userId).claim("name", name).issuedAt(new Date(now))
				.expiration(new Date(now + TOKEN_VALIDITY_MS)).signWith(key).compact();
	}

	public Claims parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
