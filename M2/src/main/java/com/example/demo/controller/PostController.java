package com.example.demo.controller;

import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid; // Import @Valid for DTO validation
import org.slf4j.Logger; // Add Logging
import org.slf4j.LoggerFactory; // Add Logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // For validating path/request params
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@Validated // Enables validation of path variables and request parameters
public class PostController extends BaseController { // Inherit getCurrentUserId

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    // Constructor Injection
    public PostController(PostService postService) {
        super(); // Call BaseController constructor
        this.postService = postService;
    }

    // --- Create Post ---
    @PostMapping
    public ResponseEntity<?> createPost(
            @Valid @ModelAttribute PostDTO postDTO, // Use @ModelAttribute for file uploads, @Valid for validation
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request); // Get user ID from interceptor attribute
            log.info("Received request to create post from user {}", userId);
            PostViewDTO createdPost = postService.createPost(postDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (UserException e) {
            log.warn("UserException during post creation: {}", e.getMessage());
            // Determine status based on error message content (improve error handling for specific cases)
            HttpStatus status = e.getMessage().toLowerCase().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            if (e.getMessage().toLowerCase().contains("authorized")) {
                status = HttpStatus.UNAUTHORIZED;
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating post: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating post: " + e.getMessage());
        }
    }

    // --- Update Post ---
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @Valid @ModelAttribute PostDTO postDTO, // Use @ModelAttribute for file uploads, @Valid for validation
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("Received request to update post {} from user {}", postId, userId);
            PostViewDTO updatedPost = postService.updatePost(postId, postDTO, userId);
            return ResponseEntity.ok(updatedPost);
        } catch (UserException e) {
            log.warn("UserException during post update for post {}: {}", postId, e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default
            String lowerCaseMsg = e.getMessage().toLowerCase();
            if (lowerCaseMsg.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lowerCaseMsg.contains("not authorized")) {
                status = HttpStatus.FORBIDDEN; // Use 403 Forbidden for auth errors
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating post: " + e.getMessage());
        }
    }

    // --- Delete Post ---
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("Received request to delete post {} from user {}", postId, userId);
            postService.deletePost(postId, userId);
            return ResponseEntity.noContent().build(); // Standard 204 No Content for successful DELETE
        } catch (UserException e) {
            log.warn("UserException during post deletion for post {}: {}", postId, e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default
            String lowerCaseMsg = e.getMessage().toLowerCase();
            if (lowerCaseMsg.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lowerCaseMsg.contains("not authorized")) {
                status = HttpStatus.FORBIDDEN; // Use 403 Forbidden
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting post: " + e.getMessage());
        }
    }

    // --- Get All Posts (Public) ---
    @GetMapping
    public ResponseEntity<List<PostViewDTO>> getAllPosts() {
        log.debug("Received request to get all posts");
        // Assuming public access - No userId needed from request
        // Error handling could be added if service throws exceptions
        List<PostViewDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    // --- Get Posts by User (Public) ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUser(@PathVariable Long userId) {
        log.debug("Received request to get posts for user {}", userId);
        try {
            // Assuming public access - No authenticated userId needed from request
            List<PostViewDTO> posts = postService.getPostsByUser(userId);
            return ResponseEntity.ok(posts);
        } catch (UserException e) {
            log.warn("UserException getting posts for user {}: {}", userId, e.getMessage());
            // Handle case where the specified userId does not exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving posts for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving posts: " + e.getMessage());
        }
    }

    // --- Get Posts by Single Hashtag (Public) ---
    @GetMapping("/hashtag/{hashtag}")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtag(
            @PathVariable String hashtag) {
        log.debug("Received request to get posts for hashtag '{}'", hashtag);
        // Assuming public access
        List<PostViewDTO> posts = postService.getPostsByHashtag(hashtag); // Service handles normalization
        return ResponseEntity.ok(posts);
    }

    // --- Get Posts by Multiple Hashtags (Public) ---
    @GetMapping("/hashtags")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtags(
            @RequestParam(required = false) List<String> tags) { // Make tags optional
        log.debug("Received request to get posts for hashtags: {}", tags);
        // Assuming public access
        if (tags == null || tags.isEmpty()) {
            log.debug("No tags provided, returning empty list.");
            return ResponseEntity.ok(List.of()); // Return empty list if no tags specified
        }
        List<PostViewDTO> posts = postService.getPostsByHashtags(tags);
        return ResponseEntity.ok(posts);
    }

    // --- Search Posts by Text (Public) ---
    @GetMapping("/search")
    public ResponseEntity<List<PostViewDTO>> searchPostsByText(
            @RequestParam String query) {
        log.debug("Received request to search posts with query: '{}'", query);
        // Assuming public access
        List<PostViewDTO> posts = postService.searchPostsByText(query);
        return ResponseEntity.ok(posts);
    }
}