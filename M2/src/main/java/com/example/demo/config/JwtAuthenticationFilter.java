package com.example.demo.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
// --- End Added Imports ---

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
// Use the correct SignatureException import for 0.12.x if needed,
// it's often under io.jsonwebtoken.security
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull; // Import NonNull for clarity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String secretString;

    private volatile SecretKey key;

    private SecretKey getKeyInstance() {
        if (this.key == null) {
            synchronized (this) {
                if (this.key == null) {
                    logger.info("M2: Creating SecretKey instance from jwt.secret property.");
                    if (secretString == null || secretString.trim().isEmpty()) {
                        logger.error("M2: jwt.secret property is null or empty. Cannot generate key.");
                        throw new IllegalStateException("M2 JWT secret is not configured properly.");
                    }
                    try {
                        byte[] keyBytes = Decoders.BASE64.decode(secretString);
                        this.key = Keys.hmacShaKeyFor(keyBytes);
                        logger.info("M2: SecretKey instance created successfully.");
                    } catch (IllegalArgumentException e) {
                        logger.error("M2: Error decoding Base64 secret or generating key: {}. Check jwt.secret format.", e.getMessage(), e);
                        throw new IllegalStateException("M2 Failed to initialize JWT key from secret.", e);
                    }
                }
            }
        }
        return this.key;
    }


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String path = request.getRequestURI();
        final String method = request.getMethod();
        logger.info("M2 JwtAuthFilter - ENTERING filter for: {} {}", method, path);

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            logger.trace("M2 JwtAuthFilter - Security context already populated, skipping JWT filter for: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.trace("M2 JwtAuthFilter - No Bearer token found for {} {}, proceeding without authentication.", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        logger.debug("M2 JwtAuthFilter - Extracted Token for {} {}: {}", method, path, jwt);

        logger.debug("M2 JwtAuthFilter - Verifying token using secret string (length {}): '{}'",
                (secretString != null ? secretString.length() : "null"),
                (secretString != null ? secretString : "null"));

        try {
            SecretKey verificationKey = getKeyInstance();

            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

            String userIdSubject = claims.getSubject();

            if (userIdSubject != null) {
                Long userId = Long.parseLong(userIdSubject);
                logger.info("M2 JwtAuthFilter - Token validated for {} {}. User ID: {}", method, path, userId);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_AUTHENTICATED_USER"))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("M2 JwtAuthFilter - SecurityContext updated for user ID {} for request {} {}", userId, method, path);

                request.setAttribute("userId", userId);
                logger.debug("M2 JwtAuthFilter - Set 'userId' request attribute to: {} for {} {}", userId, method, path);

            } else {
                logger.warn("M2 JwtAuthFilter - JWT subject (userId) is null for {} {}.", method, path);

            }

        } catch (ExpiredJwtException e) {
            logger.warn("M2 JwtAuthFilter - JWT expired for {} {}: {}", method, path, e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Token expired");
            return;
        } catch ( SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.warn("M2 JwtAuthFilter - Invalid JWT ({}) for {} {}: {}", e.getClass().getSimpleName(), method, path, e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if (e instanceof SignatureException) {
                response.getWriter().write("Unauthorized: Invalid signature");
            } else {
                response.getWriter().write("Unauthorized: Invalid token");
            }
            return;
        } catch (Exception e) {
            logger.error("M2 JwtAuthFilter - Unexpected error during JWT processing for {} {}: {}", method, path, e.getMessage(), e);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Internal Server Error during authentication processing");
            return;
        }

        logger.debug("M2 JwtAuthFilter - Proceeding with filter chain for: {} {}", method, path);
        filterChain.doFilter(request, response);
    }
}