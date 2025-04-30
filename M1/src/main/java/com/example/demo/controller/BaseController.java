package com.example.demo.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public abstract class BaseController {
    protected final RestTemplate restTemplate;

    public BaseController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected <T> HttpEntity<T> createHttpEntity(T body, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return new HttpEntity<>(body, headers);
    }

    protected HttpEntity<Void> createHttpEntity(String authHeader) {
        return createHttpEntity(null, authHeader);
    }
}