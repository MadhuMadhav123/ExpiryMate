package com.expirymate.document.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
	private static final String CLASS_NAME = JwtAuthFilter.class.getSimpleName();

	private final SecretKey key;

	public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		if (path.startsWith("/internal/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
			filterChain.doFilter(request, response);
			return;
		}

		String authorization = request.getHeader("Authorization");

		if (authorization != null && authorization.startsWith("Bearer ")) {
			try {
				Claims claims = Jwts.parser()
						.verifyWith(key)
						.build()
						.parseSignedClaims(authorization.substring(7))
						.getPayload();

				Long userId = ((Number) claims.get("userId")).longValue();
				String email = claims.getSubject();

				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(email, null, List.of());

				authentication.setDetails(userId);

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (Exception exception) {
				log.warn("{} - Invalid or expired JWT token for request path: {}, reason: {}", CLASS_NAME, path, exception.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}
}