package com.example.demo.interceptor;

// --- Added Imports for 0.12.x ---
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
// --- End Added Imports ---

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
// Use the correct SignatureException import for 0.12.x
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull; // Import NonNull
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    @Value("${jwt.secret}")
    private String secretString; // Renamed for clarity

    // Cache the generated SecretKey
    private volatile SecretKey key;

    // Helper method to get or create the SecretKey instance (thread-safe lazy initialization)
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
    public boolean preHandle( // Use @NonNull for parameters
                              @NonNull HttpServletRequest request,
                              @NonNull HttpServletResponse response,
                              @NonNull Object handler) throws Exception { // Keep throws Exception

        String path = request.getRequestURI();
        String method = request.getMethod();

        logger.info("M2 JwtInterceptor - Intercepting request: {} {}", method, path);

        // Allow OPTIONS requests pre-flight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.trace("M2 JwtInterceptor - Allowing OPTIONS request for path: {}", path);
            return true;
        }

        // Skip non-API paths if desired (adjust logic if needed)
        if (!path.startsWith("/api/")) {
            logger.trace("M2 JwtInterceptor - Skipping JWT validation for non-API path: {}", path);
            return true;
        }

        // --- Define Public API Endpoints ---
        // Allow all GET requests for posts and comments without a token
        boolean isPublicGetRequest = method.equalsIgnoreCase("GET") &&
                (path.startsWith("/api/posts") || path.startsWith("/api/comments"));

        if (isPublicGetRequest) {
            logger.trace("M2 JwtInterceptor - Skipping JWT validation for public GET endpoint: {} {}", method, path);
            // You might still want to attempt parsing if a token IS present,
            // to set the userId attribute for potential downstream use,
            // but don't fail if the token is missing or invalid for public endpoints.
            // Let's proceed to token check but handle failure gracefully for public GET.
            // return true; // <-- If you want to strictly skip ANY token validation for public GETs
        }
        // --- End Public API Endpoints ---

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.debug("M2 JwtInterceptor - Incoming Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // If it's a public GET, it's okay to not have a token. Allow request.
            if (isPublicGetRequest) {
                logger.trace("M2 JwtInterceptor - No token present for public GET {}. Allowing.", path);
                return true;
            }
            // Otherwise, for protected endpoints, token is required.
            logger.warn("M2 JwtInterceptor - Missing or invalid Authorization header for protected path: {} {}", method, path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Missing or invalid authorization token");
            return false; // Block request
        }

        // Token is present, attempt validation
        String token = authHeader.substring(7);
        logger.debug("M2 JwtInterceptor - Extracted Token: {}", token);

        try {
            SecretKey verificationKey = getKeyInstance(); // Get the SecretKey

            // *** Use 0.12.x verification style ***
            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey) // Use verifyWith(SecretKey)
                    .build()
                    .parseSignedClaims(token)     // Use parseSignedClaims
                    .getPayload();                // Get claims

            String userIdSubject = claims.getSubject();
            if (userIdSubject == null) {
                // Even for public GETs, if a token is provided, it should be valid minimally (e.g., have a subject)
                logger.warn("M2 JwtInterceptor - JWT subject (userId) is null in provided token for {}.", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value()); // Or maybe 400 Bad Request
                response.getWriter().write("Unauthorized: Invalid token payload (missing subject)");
                return false;
            }

            logger.info("M2 JwtInterceptor - Token validated. User ID (Subject): {}", userIdSubject);

            // Set userId attribute regardless of public/private (if token was valid)
            Long userId = Long.parseLong(userIdSubject);
            request.setAttribute("userId", userId);
            logger.debug("M2 JwtInterceptor - Set 'userId' attribute to: {}", userId);
            return true; // Token valid, allow request

        } catch (ExpiredJwtException e) {
            logger.warn("M2 JwtInterceptor - JWT expired for {}: {}", path, e.getMessage());
            // If it's public GET, maybe allow? Or enforce validity if token IS present?
            // For now, let's reject expired tokens even on public GETs if they are provided.
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Token expired");
            return false;
        } catch ( SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.warn("M2 JwtInterceptor - Invalid JWT ({}) for {}: {}", e.getClass().getSimpleName(), path, e.getMessage());
            // Reject invalid tokens even on public GETs if they are provided.
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if (e instanceof SignatureException) {
                response.getWriter().write("Unauthorized: Invalid signature");
            } else {
                response.getWriter().write("Unauthorized: Invalid token format/type");
            }
            return false;
        } catch (Exception e) {
            logger.error("M2 JwtInterceptor - Unexpected error during JWT validation for {}: {}", path, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Internal Server Error during authentication");
            return false;
        }
        // The logic should have returned true or false within the if/try-catch block.
        // This part should ideally not be reached if a token was processed.
        // If it reaches here, it means no token was found AND it wasn't a public GET.
        // The check for missing header already handles this case above.
        // However, keeping a fallback return just in case.
        // return true; // Or false depending on default desired behavior
    }
}