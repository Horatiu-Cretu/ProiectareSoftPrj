package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // Indicates this class provides bean definitions
public class AppConfig {

    @Bean // Declares that this method produces a bean to be managed by Spring
    public RestTemplate restTemplate() {
        // Creates and returns a RestTemplate instance
        return new RestTemplate();
    }
}