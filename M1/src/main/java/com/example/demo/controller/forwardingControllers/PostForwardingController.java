package com.example.demo.controller.forwardingControllers;

import com.example.demo.dto.PostDTO; // Import the new M1 PostDTO
import com.example.demo.dto.PostViewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource; // Needed for forwarding file
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap; // Needed for multipart body
import org.springframework.util.MultiValueMap; // Needed for multipart body
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile; // Needed for file uploads

import java.io.IOException; // Needed for getBytes()
import java.util.List;
// Removed unused Map import

@RestController
@RequestMapping("/api/posts")
public class PostForwardingController extends BaseForwardingController {

    private static final Logger log = LoggerFactory.getLogger(PostForwardingController.class);

    private final RestTemplate restTemplate;
    private final String m2BaseUrl;
    private static final String SERVICE_PATH = "/api/posts";

    public PostForwardingController(
            @Value("${m2.service.url}") String m2ServiceUrl,
            RestTemplate restTemplate) {
        super(m2ServiceUrl, SERVICE_PATH);
        this.m2BaseUrl = m2ServiceUrl;
        this.restTemplate = restTemplate;
    }

    // --- createPost modified for multipart/form-data ---
    @PostMapping
    public ResponseEntity<?> createPost(
            @ModelAttribute PostDTO postDTO, // <-- Changed to @ModelAttribute, uses M1's PostDTO
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String targetUrl = m2BaseUrl + SERVICE_PATH + "";
        log.info("Forwarding POST request (multipart) for post creation to M2: {}", targetUrl);

        // Prepare Headers FOR MULTIPART FORWARDING
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // Set content type for forwarding
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            log.debug("Forwarding Authorization header: {}", authHeader);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // Still expect JSON back

        // --- Create Multipart Request Body ---
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("content", postDTO.getContent());
        body.add("postType", postDTO.getPostType().name()); // Send enum name as string

        // Add hashtags - M2 likely expects them as repeated params or needs parsing logic
        if (postDTO.getHashtags() != null) {
            postDTO.getHashtags().forEach(tag -> body.add("hashtags", tag));
        }

        // Add image file if present
        if (postDTO.getImage() != null && !postDTO.getImage().isEmpty()) {
            try {
                // Need to wrap the byte[] in a resource with a filename for RestTemplate
                ByteArrayResource resource = new ByteArrayResource(postDTO.getImage().getBytes()) {
                    @Override
                    public String getFilename() {
                        // Provide filename, otherwise RestTemplate might use a generic one
                        return postDTO.getImage().getOriginalFilename();
                    }
                };
                body.add("image", resource); // Add the file resource
            } catch (IOException e) {
                log.error("Error reading image file bytes for forwarding: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image file.");
            }
        }

        // Create Request Entity with Multipart Body
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // --- Direct RestTemplate Call with Multipart Entity ---
            ResponseEntity<PostViewDTO> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    requestEntity, // Send the multipart request
                    PostViewDTO.class
            );
            log.info("M2 responded to POST with status: {}", response.getStatusCode());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Client error from M2 during POST forward: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error from M2 during POST forward: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("RestClientException during POST forward to M2: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Post service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception during POST forward: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error during forwarding: " + e.getMessage());
        }
    }

    // --- Helper to create Headers (mostly for GET now) ---
    private HttpHeaders createHeadersInline(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            log.debug("Forwarding Authorization header: {}", authHeader);
        }
        // Accept JSON for GET responses
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // --- PUT method ---
    // NOTE: PUT also likely needs similar multipart handling if M2's update uses @ModelAttribute
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost( // Changed return type to ResponseEntity<?>
                                         @PathVariable Long postId,
                                         @ModelAttribute PostDTO postDTO, // <--- Changed to @ModelAttribute
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Refactoring needed here similar to POST to send multipart/form-data
        log.warn("PUT /api/posts/{} forwarding not yet updated for multipart/form-data. Using potentially incorrect base class method.", postId);
        // Temporarily return an error until refactored
        // return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("PUT forwarding for multipart not yet implemented.");

        // --- Refactored PUT logic (similar to POST) ---
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/" + postId;
        log.info("Forwarding PUT request (multipart) for post update to M2: {}", targetUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("content", postDTO.getContent());
        body.add("postType", postDTO.getPostType().name());
        if (postDTO.getHashtags() != null) {
            postDTO.getHashtags().forEach(tag -> body.add("hashtags", tag));
        }
        if (postDTO.getImage() != null && !postDTO.getImage().isEmpty()) {
            try {
                ByteArrayResource resource = new ByteArrayResource(postDTO.getImage().getBytes()) {
                    @Override
                    public String getFilename() { return postDTO.getImage().getOriginalFilename(); }
                };
                body.add("image", resource);
            } catch (IOException e) {
                log.error("Error reading image file bytes for PUT forwarding: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image file.");
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PostViewDTO> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.PUT, // Use PUT method
                    requestEntity,
                    PostViewDTO.class
            );
            log.info("M2 responded to PUT with status: {}", response.getStatusCode());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Client error from M2 during PUT forward: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } // ... (add other catch blocks similar to POST)
        catch (RestClientException e) {
            log.error("RestClientException during PUT forward to M2: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Post service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception during PUT forward: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error during forwarding: " + e.getMessage());
        }
    }


    // --- GET/DELETE/SEARCH methods remain mostly the same ---
    // (Code for GETs/DELETE/Search from previous response omitted for brevity,
    // ensure they use the injected restTemplate and correct error handling)
    @GetMapping
    public ResponseEntity<List<PostViewDTO>> getAllPosts(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "";
        log.debug("Forwarding GET all posts request to {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            log.error("RestClientException forwarding GET all posts request: {}", e.getMessage());
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected error forwarding GET all posts request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostViewDTO>> getPostsByUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/user/" + userId;
        log.debug("Forwarding GET posts by user request to {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            log.error("RestClientException forwarding GET posts by user request: {}", e.getMessage());
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected error forwarding GET posts by user request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/hashtag/{hashtag}")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/hashtag/" + hashtag;
        log.debug("Forwarding GET posts by hashtag request to {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            log.error("RestClientException forwarding GET posts by hashtag request: {}", e.getMessage());
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected error forwarding GET posts by hashtag request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/hashtags")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtags(
            @RequestParam List<String> tags,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(m2BaseUrl + SERVICE_PATH + "/hashtags");
        tags.forEach(tag -> builder.queryParam("tags", tag));
        String targetUrl = builder.toUriString();

        log.debug("Forwarding GET posts by hashtags request to {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            log.error("RestClientException forwarding GET posts by hashtags request: {}", e.getMessage());
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected error forwarding GET posts by hashtags request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost( // Return type should be Void for DELETE
                                            @PathVariable Long postId,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Refactor DELETE similarly or keep using super.forwardDelete
        log.debug("Forwarding DELETE request via super.forwardDelete for post {}", postId);
        try {
            return super.forwardDelete("/" + postId, authHeader);
        } catch (HttpClientErrorException e) {
            log.error("Client error during DELETE forward: {}", e.getStatusCode());
            // Need to return ResponseEntity<Void>
            return ResponseEntity.status(e.getStatusCode()).build();
        } // Add other catch blocks if needed
        catch (RestClientException e) {
            log.error("RestClientException during DELETE forward: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostViewDTO>> searchPostsByText(
            @RequestParam String query,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(m2BaseUrl + SERVICE_PATH + "/search")
                .queryParam("query", query);
        String targetUrl = builder.toUriString();

        log.debug("Forwarding GET search posts request to {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            log.error("RestClientException forwarding GET search posts request: {}", e.getMessage());
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected error forwarding GET search posts request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}