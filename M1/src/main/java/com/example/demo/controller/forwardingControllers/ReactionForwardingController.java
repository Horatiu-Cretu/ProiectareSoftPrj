package com.example.demo.controller.forwardingControllers;

import com.example.demo.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
public class ReactionForwardingController {

    private static final Logger log = LoggerFactory.getLogger(ReactionForwardingController.class);
    private final RestTemplate restTemplate;
    private final String m3BaseUrl;
    private final JWTService jwtService;

    public static final String USER_ID_HEADER = "X-User-ID";

    public ReactionForwardingController(@Value("${m3.service.url}") String m3ServiceUrl,
                                        RestTemplate restTemplate,
                                        JWTService jwtService) {
        this.m3BaseUrl = m3ServiceUrl + "/api/reactions";
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
    }

    private HttpHeaders createHeadersWithAuthAndUserId(String authHeader) {
        HttpHeaders headers = new HttpHeaders();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = jwtService.extractUserId(token);
                if (userId != null) {
                    headers.set(USER_ID_HEADER, String.valueOf(userId));
                    log.info("Forwarding request to M3 with X-User-ID: {}", userId);
                } else {
                    log.warn("User ID extracted from token is null. X-User-ID header will not be set.");
                }
            } catch (Exception e) {
                log.warn("Could not extract userId from token for forwarding to M3: {}. X-User-ID header will not be set.", e.getMessage());
            }
        } else {
            log.warn("Authorization header is missing or not a Bearer token. X-User-ID header will not be set for M3.");
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @PostMapping
    public ResponseEntity<?> forwardPostReaction(@RequestBody Object payload,
                                                 @RequestHeader(value = "Authorization") String authHeader) {
        HttpHeaders headers = createHeadersWithAuthAndUserId(authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
        String targetUrl = m3BaseUrl;
        log.info("Forwarding POST request to M3: {} with payload and X-User-ID header if available", targetUrl);

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity, Object.class);
        } catch (HttpClientErrorException e) {
            log.error("Client error from M3: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error from M3: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("RestClientException during POST forward to M3: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Reaction service: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> forwardDeleteReaction(@RequestParam Long targetId,
                                                   @RequestParam String targetType,
                                                   @RequestHeader(value = "Authorization") String authHeader) {
        HttpHeaders headers = createHeadersWithAuthAndUserId(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        String targetUrl = UriComponentsBuilder.fromHttpUrl(m3BaseUrl)
                .queryParam("targetId", targetId)
                .queryParam("targetType", targetType)
                .toUriString();

        log.info("Forwarding DELETE request to M3: {} with X-User-ID header if available", targetUrl);

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.DELETE, requestEntity, Object.class);
        } catch (HttpClientErrorException e) {
            log.error("Client error from M3: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error from M3: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("RestClientException during DELETE forward to M3: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Reaction service: " + e.getMessage());
        }
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<?> getReactionsForTarget(@PathVariable String targetType,
                                                   @PathVariable Long targetId,
                                                   @RequestHeader(value="Authorization") String authHeader) {
        HttpHeaders headers = createHeadersWithAuthAndUserId(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        String targetUrl = m3BaseUrl + "/target/" + targetType + "/" + targetId;
        log.info("Forwarding GET request to M3: {} with X-User-ID header if available", targetUrl);
        try {
            return restTemplate.exchange(targetUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<List<?>>() {});
        } catch (HttpClientErrorException e) {
            log.error("Client error from M3 (getReactionsForTarget): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error from M3 (getReactionsForTarget): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("RestClientException during GET (getReactionsForTarget) forward to M3: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Reaction service (GET reactions).");
        }
    }

    @GetMapping("/target/{targetType}/{targetId}/count")
    public ResponseEntity<?> getReactionCountForTarget(@PathVariable String targetType,
                                                       @PathVariable Long targetId,
                                                       @RequestHeader(value="Authorization") String authHeader) {
        HttpHeaders headers = createHeadersWithAuthAndUserId(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        String targetUrl = m3BaseUrl + "/target/" + targetType + "/" + targetId + "/count";
        log.info("Forwarding GET request to M3: {} with X-User-ID header if available", targetUrl);
        try {
            return restTemplate.exchange(targetUrl, HttpMethod.GET, requestEntity, Long.class);
        } catch (HttpClientErrorException e) {
            log.error("Client error from M3 (getReactionCountForTarget): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error from M3 (getReactionCountForTarget): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("RestClientException during GET (getReactionCountForTarget) forward to M3: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Reaction service (GET count).");
        }
    }
}