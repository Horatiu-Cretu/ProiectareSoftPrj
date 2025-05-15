package com.example.demo.controller.forwardingControllers;

import com.example.demo.dto.forwardingdto.PostDTO;
import com.example.demo.dto.forwardingdto.PostViewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
// import org.springframework.web.multipart.MultipartFile; // Nu mai este necesar aici daca PostDTO il are

import java.io.IOException;
import java.util.List;

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

    @PostMapping
    public ResponseEntity<?> createPost(
            @ModelAttribute PostDTO postDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String targetUrl = m2BaseUrl + SERVICE_PATH + "";
        log.info("Forwarding POST request (multipart) for post creation to M2: {}", targetUrl);

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
                    public String getFilename() {
                        return postDTO.getImage().getOriginalFilename();
                    }
                };
                body.add("image", resource);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image file.");
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PostViewDTO> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PostViewDTO.class
            );
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Post service: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error during forwarding: " + e.getMessage());
        }
    }

    private HttpHeaders createHeadersInline(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @ModelAttribute PostDTO postDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/" + postId;
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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image file.");
            }
        }
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<PostViewDTO> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.PUT,
                    requestEntity,
                    PostViewDTO.class
            );
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(e.getResponseBodyAsString());
        }
        catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error connecting to Post service: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error during forwarding: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PostViewDTO>> getAllPosts(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "";
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-reaction-count")
    public ResponseEntity<List<PostViewDTO>> getAllPostsSortedByReactions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/by-reaction-count";
        log.info("Forwarding GET request for posts sorted by reactions to M2: {}", targetUrl);
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (HttpClientErrorException e) {
            log.error("Client error from M2 (by-reaction-count): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(null);
        } catch (HttpServerErrorException e) {
            log.error("Server error from M2 (by-reaction-count): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(e.getResponseHeaders().getContentType()).body(null);
        } catch (RestClientException e) {
            log.error("RestClientException during GET (by-reaction-count) forward to M2: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        }
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostViewDTO>> getPostsByUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = createHeadersInline(authHeader);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String targetUrl = m2BaseUrl + SERVICE_PATH + "/user/" + userId;
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
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
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
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
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            return super.forwardDelete( "/" + postId, authHeader);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
        catch (RestClientException e) {
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
        try {
            return restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<PostViewDTO>>() {}
            );
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException hce) {
                return ResponseEntity.status(hce.getStatusCode()).contentType(hce.getResponseHeaders().getContentType()).body(null);
            } else if (e instanceof HttpServerErrorException hse) {
                return ResponseEntity.status(hse.getStatusCode()).contentType(hse.getResponseHeaders().getContentType()).body(null);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}