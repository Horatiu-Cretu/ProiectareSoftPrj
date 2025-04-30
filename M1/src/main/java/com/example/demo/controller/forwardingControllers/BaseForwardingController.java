package com.example.demo.controller.forwardingControllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper; // Keep if needed for other purposes
import java.util.Map; // Keep if needed for other purposes
import java.util.Base64; // Keep if needed for other purposes

public abstract class BaseForwardingController {
    protected final RestTemplate restTemplate;
    protected final String SERVICE_PATH;
    protected final String baseUrl;

    // ObjectMapper and jwtSecret might not be needed here anymore unless used elsewhere
    // private final ObjectMapper objectMapper = new ObjectMapper();
    // @Value("${jwt.secret}")
    // private String jwtSecret;

    protected BaseForwardingController(String baseUrl, String servicePath) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.SERVICE_PATH = servicePath;
    }

    // Generic forwarding method for POST requests
    protected <T> ResponseEntity<T> forwardPost(
            String path,
            Object body,
            String authHeader,
            Class<T> responseType) {
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(
                    baseUrl + SERVICE_PATH + path,
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );
        } catch (RestClientException e) {
            System.err.println("Error forwarding POST request: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpClientErrorException) e).getStatusCode()).build();
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpServerErrorException) e).getStatusCode()).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding POST request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Generic forwarding method for PUT requests
    protected <T> ResponseEntity<T> forwardPut(
            String path,
            Object body,
            String authHeader,
            Class<T> responseType) {
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(
                    baseUrl + SERVICE_PATH + path,
                    HttpMethod.PUT,
                    requestEntity,
                    responseType
            );
        } catch (RestClientException e) {
            System.err.println("Error forwarding PUT request: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpClientErrorException) e).getStatusCode()).build();
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpServerErrorException) e).getStatusCode()).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding PUT request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Generic forwarding method for GET requests
    protected <T> ResponseEntity<T> forwardGet(
            String path,
            String authHeader,
            Class<T> responseType) {
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(
                    baseUrl + SERVICE_PATH + path,
                    HttpMethod.GET,
                    requestEntity,
                    responseType
            );
        } catch (RestClientException e) {
            System.err.println("Error forwarding GET request: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpClientErrorException) e).getStatusCode()).build();
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpServerErrorException) e).getStatusCode()).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding GET request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Forwarding method for DELETE requests
    protected ResponseEntity<Void> forwardDelete(
            String path,
            String authHeader) {
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(
                    baseUrl + SERVICE_PATH + path,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );
        } catch (RestClientException e) {
            System.err.println("Error forwarding DELETE request: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpClientErrorException) e).getStatusCode()).build();
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                return ResponseEntity.status(((org.springframework.web.client.HttpServerErrorException) e).getStatusCode()).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding DELETE request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // *** CHANGE HERE: Remove email extraction and X-User-Email header ***
    HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Pass the original Authorization header only
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            System.out.println("Forwarding Authorization header: " + authHeader); // Optional logging
        }
        // Set content type if necessary, e.g., for POST/PUT
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Add other headers if needed (e.g., Accept)
        // headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // *** REMOVED: extractEmailFromToken method is no longer needed here ***
    // private String extractEmailFromToken(String token) { ... }
}