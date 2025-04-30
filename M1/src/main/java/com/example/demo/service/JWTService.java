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

    // Ensure this value is being loaded correctly
    @Value("${jwt.secret}")
    private String secretKey;

    // Keep track of the generated key to avoid regenerating it constantly (optional optimization)
    private volatile SecretKey key = null;

    // Accepts userId (Long) AND email (String)
    public String generateToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        logger.info("Generating token for user ID: {}, email: {}", userId, email);

        // Ensure key is generated before signing
        SecretKey currentKey = getKey();
        if (currentKey == null) {
            // Log the error specifically
            logger.error("Failed to generate JWT: SecretKey is null. Check jwt.secret property and potential generation errors.");
            throw new RuntimeException("Failed to generate JWT due to missing secret key.");
        }


        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000000000))
                .signWith(currentKey) // Use the generated SecretKey
                .compact();
    }

    // Method to get or generate the key
    // Make it synchronized to handle concurrent access during first generation
    private synchronized SecretKey getKey() {
        // Double-checked locking for efficiency (optional)
        if (key == null) {
            synchronized (this) {
                if (key == null) {
                    logger.info("Attempting to generate SecretKey from jwt.secret property.");
                    if (secretKey == null || secretKey.trim().isEmpty()) {
                        logger.error("jwt.secret property is null or empty. Cannot generate key.");
                        // Optionally throw here, or let it return null and handle in generateToken
                        return null; // Return null if secret is invalid
                    }
                    logger.debug("Using secret string (length {}): '{}'", secretKey.length(), secretKey); // Be cautious logging secrets even in debug
                    try {
                        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
                        logger.debug("Decoded secret key bytes (length {}).", keyBytes.length);
                        // Check if key length is sufficient for HS256 (minimum 256 bits / 32 bytes)
                        if (keyBytes.length < 32) {
                            logger.warn("Decoded JWT secret key length is less than 32 bytes ({}), which is potentially insecure for HS256.", keyBytes.length);
                        }
                        this.key = Keys.hmacShaKeyFor(keyBytes);
                        logger.info("SecretKey generated successfully.");
                    } catch (IllegalArgumentException e) {
                        // Catch errors during Base64 decoding or key generation
                        logger.error("Error generating SecretKey: {}. Check if jwt.secret is valid Base64 and appropriate length.", e.getMessage(), e);
                        // Optionally rethrow or return null
                        return null; // Indicate failure
                    } catch (Exception e) {
                        // Catch any other unexpected errors
                        logger.error("Unexpected error during SecretKey generation: {}", e.getMessage(), e);
                        return null;
                    }
                }
            }
        }
        return key;
    }


    // Simplified validation: checks signature and expiration
    public boolean validateToken(String jwtToken, UserDetails userDetails) {
        try {
            // Use the same getKey() method to ensure consistency
            SecretKey currentKey = getKey();
            if (currentKey == null) {
                logger.error("Token validation failed: Secret key is null.");
                return false;
            }

            // Use verifyWith for JJWT 0.12.x (as used in M1)
            Jwts.parser()
                    .verifyWith(currentKey) // Use verifyWith for M1's JJWT version
                    .build()
                    .parseSignedClaims(jwtToken); // This call implicitly validates signature and expiration

            logger.debug("Token claims validated successfully.");

            // Optional: Re-check expiration explicitly (though parseSignedClaims should do it)
            if (isTokenExpired(jwtToken)) {
                logger.warn("Token validation failed: Token is expired.");
                return false;
            }

            // Optional: Compare token subject/email with UserDetails if needed
            // String tokenEmail = extractEmail(jwtToken);
            // if (!userDetails.getUsername().equals(tokenEmail)) {
            //    logger.warn("Token validation failed: Email mismatch.");
            //    return false;
            // }

            return true; // If parsing succeeded and not expired
        } catch (Exception e) {
            // Log the specific exception (e.g., SignatureException, ExpiredJwtException)
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }


    private boolean isTokenExpired(String jwtToken) {
        try {
            return extractExpiration(jwtToken).before(new Date());
        } catch (Exception e) {
            logger.error("Could not extract expiration from token: {}", e.getMessage());
            return true; // Treat as expired if extraction fails
        }
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaim(jwtToken, Claims::getExpiration);
    }

    // Extract subject (which is the User ID as String)
    public String extractSubject(String jwtToken) {
        return extractClaim(jwtToken, Claims::getSubject);
    }

    // Extract User ID as Long from the subject
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

    // Extract email from the custom claim
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


    // Generic method to extract a specific claim using a resolver function
    private <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwtToken);
        return claimsResolver.apply(claims);
    }

    // Extracts all claims, implicitly validating signature and structure
    private Claims extractAllClaims(String jwtToken) {
        SecretKey currentKey = getKey();
        if (currentKey == null) {
            logger.error("Cannot extract claims: Secret key is null.");
            throw new RuntimeException("Cannot process JWT: Secret key is missing or invalid.");
        }
        // Use verifyWith for JJWT 0.12.x (as used in M1)
        return Jwts.parser()
                .verifyWith(currentKey) // Use verifyWith for M1's JJWT version
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }
}