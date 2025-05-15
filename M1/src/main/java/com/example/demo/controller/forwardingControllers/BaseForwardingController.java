package com.example.demo.controller.forwardingControllers;

import org.springframework.core.ParameterizedTypeReference; // <-- Add this import
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;


public abstract class BaseForwardingController {
    protected final RestTemplate restTemplate;
    protected final String SERVICE_PATH;
    protected final String baseUrl;

    protected BaseForwardingController(String baseUrl, String servicePath) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.SERVICE_PATH = servicePath;
    }

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
                org.springframework.web.client.HttpClientErrorException hce = (org.springframework.web.client.HttpClientErrorException) e;
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException hse = (org.springframework.web.client.HttpServerErrorException) e;
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding POST request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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
                org.springframework.web.client.HttpClientErrorException hce = (org.springframework.web.client.HttpClientErrorException) e;
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException hse = (org.springframework.web.client.HttpServerErrorException) e;
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding PUT request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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
            System.err.println("Error forwarding GET request (Class type): " + e.getMessage());
            e.printStackTrace();
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException hce = (org.springframework.web.client.HttpClientErrorException) e;
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException hse = (org.springframework.web.client.HttpServerErrorException) e;
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding GET request (Class type): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    protected <T> ResponseEntity<T> forwardGet(
            String path,
            String authHeader,
            ParameterizedTypeReference<T> responseType) {
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
            System.err.println("Error forwarding GET request (ParameterizedTypeReference): " + e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException hce = (org.springframework.web.client.HttpClientErrorException) e;
                System.err.println("Client error body: " + hce.getResponseBodyAsString());
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).build(); // Or .body(null)
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException hse = (org.springframework.web.client.HttpServerErrorException) e;
                System.err.println("Server error body: " + hse.getResponseBodyAsString());
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).build(); // Or .body(null)
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error forwarding GET request (ParameterizedTypeReference): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            System.out.println("Forwarding Authorization header: " + authHeader);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}