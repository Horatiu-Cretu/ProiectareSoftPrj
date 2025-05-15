package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.Key; // Import Key interface
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    private volatile SecretKey key = null;

    public String generateToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        logger.info("Generating token for user ID: {}, email: {}", userId, email);

        SecretKey currentKey = getKey();
        if (currentKey == null) {
            logger.error("Failed to generate JWT: SecretKey is null. Check jwt.secret property and potential generation errors.");
            throw new RuntimeException("Failed to generate JWT due to missing secret key.");
        }


        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000000000))
                .signWith(currentKey)
                .compact();
    }

    private synchronized SecretKey getKey() {
        if (key == null) {
            synchronized (this) {
                if (key == null) {
                    logger.info("Attempting to generate SecretKey from jwt.secret property.");
                    if (secretKey == null || secretKey.trim().isEmpty()) {
                        logger.error("jwt.secret property is null or empty. Cannot generate key.");
                        return null;
                    }
                    logger.debug("Using secret string (length {}): '{}'", secretKey.length(), secretKey);
                    try {
                        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
                        logger.debug("Decoded secret key bytes (length {}).", keyBytes.length);
                        if (keyBytes.length < 32) {
                            logger.warn("Decoded JWT secret key length is less than 32 bytes ({}), which is potentially insecure for HS256.", keyBytes.length);
                        }
                        this.key = Keys.hmacShaKeyFor(keyBytes);
                        logger.info("SecretKey generated successfully.");
                    } catch (IllegalArgumentException e) {
                        logger.error("Error generating SecretKey: {}. Check if jwt.secret is valid Base64 and appropriate length.", e.getMessage(), e);
                        return null;
                    } catch (Exception e) {
                        logger.error("Unexpected error during SecretKey generation: {}", e.getMessage(), e);
                        return null;
                    }
                }
            }
        }
        return key;
    }


    public boolean validateToken(String jwtToken, UserDetails userDetails) {
        try {
            SecretKey currentKey = getKey();
            if (currentKey == null) {
                logger.error("Token validation failed: Secret key is null.");
                return false;
            }

            Jwts.parser()
                    .verifyWith(currentKey)
                    .build()
                    .parseSignedClaims(jwtToken);

            logger.debug("Token claims validated successfully.");

            if (isTokenExpired(jwtToken)) {
                logger.warn("Token validation failed: Token is expired.");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }


    private boolean isTokenExpired(String jwtToken) {
        try {
            return extractExpiration(jwtToken).before(new Date());
        } catch (Exception e) {
            logger.error("Could not extract expiration from token: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaim(jwtToken, Claims::getExpiration);
    }

    public String extractSubject(String jwtToken) {
        return extractClaim(jwtToken, Claims::getSubject);
    }

    public Long extractUserId(String jwtToken) {
        try {
            String subject = extractSubject(jwtToken);
            if (subject == null) {
                logger.error("Token subject (user ID) is null.");
                throw new RuntimeException("Token subject (user ID) is null");
            }
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            logger.error("Invalid user ID format '{}' in token subject.", extractSubject(jwtToken), e);
            throw new RuntimeException("Invalid user ID in token subject", e);
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract user ID from token", e);
        }
    }

    public String extractEmail(String jwtToken) {
        try {
            Claims claims = extractAllClaims(jwtToken);
            String email = claims.get("email", String.class);
            if (email == null) {
                logger.warn("Email claim not found or not a String in token for subject {}", claims.getSubject());
            }
            return email;
        } catch (Exception e) {
            logger.error("Failed to extract email claim from token: {}", e.getMessage(), e);
            return null;
        }
    }


    private <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwtToken);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String jwtToken) {
        SecretKey currentKey = getKey();
        if (currentKey == null) {
            logger.error("Cannot extract claims: Secret key is null.");
            throw new RuntimeException("Cannot process JWT: Secret key is missing or invalid.");
        }
        return Jwts.parser()
                .verifyWith(currentKey)
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }
}