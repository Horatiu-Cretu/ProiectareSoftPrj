package com.example.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;


@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    @Value("${jwt.secret}")
    private String secretString;

    private volatile SecretKey key;

    private SecretKey getKeyInstance() {
        if (this.key == null) {
            synchronized (this) {
                if (this.key == null) {
                    logger.info("M2 Interceptor: Creating SecretKey instance from jwt.secret property.");
                    if (secretString == null || secretString.trim().isEmpty()) {
                        logger.error("M2 Interceptor: jwt.secret property is null or empty.");
                        throw new IllegalStateException("M2 Interceptor: JWT secret is not configured properly.");
                    }
                    try {
                        byte[] keyBytes = Decoders.BASE64.decode(secretString);
                        this.key = Keys.hmacShaKeyFor(keyBytes);
                        logger.info("M2 Interceptor: SecretKey instance created successfully.");
                    } catch (IllegalArgumentException e) {
                        logger.error("M2 Interceptor: Error decoding/generating key: {}. Check jwt.secret.", e.getMessage(), e);
                        throw new IllegalStateException("M2 Interceptor: Failed to initialize JWT key.", e);
                    }
                }
            }
        }
        return this.key;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        logger.info("M2 JwtInterceptor - Intercepting request: {} {}", method, path);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.trace("M2 JwtInterceptor - Allowing OPTIONS request for path: {}", path);
            return true;
        }

        if (method.equalsIgnoreCase("PUT") &&
                (path.matches("/api/m2/posts/.*/update-reaction-count") ||
                        path.matches("/api/m2/comments/.*/update-reaction-count"))) {
            logger.info("M2 JwtInterceptor - Allowing internal PUT request to update-reaction-count path: {}", path);
            return true;
        }
        if (!path.startsWith("/api/")) {
            logger.trace("M2 JwtInterceptor - Skipping JWT validation for non-API path: {}", path);
            return true;
        }

        boolean isPublicGetRequest = method.equalsIgnoreCase("GET") &&
                (path.startsWith("/api/posts") || path.startsWith("/api/comments"));

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.debug("M2 JwtInterceptor - Incoming Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isPublicGetRequest) {
                logger.trace("M2 JwtInterceptor - No token present for public GET {}. Allowing.", path);
                return true;
            }
            logger.warn("M2 JwtInterceptor - Missing or invalid Authorization header for protected path: {} {}", method, path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Missing or invalid authorization token");
            return false;
        }

        String token = authHeader.substring(7);
        logger.debug("M2 JwtInterceptor - Extracted Token: {}", token);

        try {
            SecretKey verificationKey = getKeyInstance();

            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userIdSubject = claims.getSubject();
            if (userIdSubject == null) {
                logger.warn("M2 JwtInterceptor - JWT subject (userId) is null in provided token for {}.", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Unauthorized: Invalid token payload (missing subject)");
                return false;
            }

            logger.info("M2 JwtInterceptor - Token validated. User ID (Subject): {}", userIdSubject);
            Long userId = Long.parseLong(userIdSubject);
            request.setAttribute("userId", userId);
            logger.debug("M2 JwtInterceptor - Set 'userId' attribute to: {}", userId);
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("M2 JwtInterceptor - JWT expired for {}: {}", path, e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Token expired");
            return false;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.warn("M2 JwtInterceptor - Invalid JWT ({}) for {}: {}", e.getClass().getSimpleName(), path, e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Invalid token format/type or signature");
            return false;
        } catch (Exception e) {
            logger.error("M2 JwtInterceptor - Unexpected error during JWT validation for {}: {}", path, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Internal Server Error during authentication");
            return false;
        }
    }
}