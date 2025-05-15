package com.example.demo.controller;

import com.example.demo.dto.ReactionCountUpdateDTO;
import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/m2/posts")
@Validated
public class PostController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    public PostController(PostService postService) {
        super();
        this.postService = postService;
    }

    private ResponseEntity<?> handleUserException(UserException e, String actionContext) {
        log.error("Error during {}: {}", actionContext, e.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String lowerCaseMsg = e.getMessage().toLowerCase();

        if (lowerCaseMsg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (lowerCaseMsg.contains("not authorized") || lowerCaseMsg.contains("unauthorized")) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(e.getMessage());
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @Valid @ModelAttribute PostDTO postDTO,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            PostViewDTO createdPost = postService.createPost(postDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (UserException e) {
            return handleUserException(e, "creating post for user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error creating post for user {}: {}", request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the post.");
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @Valid @ModelAttribute PostDTO postDTO,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            PostViewDTO updatedPost = postService.updatePost(postId, postDTO, userId);
            return ResponseEntity.ok(updatedPost);
        } catch (UserException e) {
            return handleUserException(e, "updating post " + postId + " by user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error updating post {} by user {}: {}", postId, request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while updating the post.");
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            postService.deletePost(postId, userId);
            return ResponseEntity.noContent().build();
        } catch (UserException e) {
            return handleUserException(e, "deleting post " + postId + " by user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error deleting post {} by user {}: {}", postId, request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while deleting the post.");
        }
    }

    @DeleteMapping("/admin/posts/{postId}")
    public ResponseEntity<?> deletePostAsAdmin(
            @PathVariable Long postId,
            HttpServletRequest request) {
        try {
            Long adminUserId = getCurrentUserId(request);
            log.info("Admin {} attempting to delete post {} via M2 endpoint.", adminUserId, postId);
            postService.deletePostAsAdmin(postId, adminUserId);
            return ResponseEntity.noContent().build();
        } catch (UserException e) {
            log.error("Admin (ID: {}) error deleting post {}: {}", request.getAttribute("userId"), postId, e.getMessage());
            return handleUserException(e, "admin deleting post " + postId);
        } catch (Exception e) {
            log.error("Unexpected admin error deleting post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during admin deletion of the post.");
        }
    }

    @GetMapping
    public ResponseEntity<List<PostViewDTO>> getAllPosts() {
        List<PostViewDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/by-reaction-count")
    public ResponseEntity<List<PostViewDTO>> getAllPostsOrderByReactionCount() {
        List<PostViewDTO> posts = postService.getAllPostsOrderByReactionCountDesc();
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{postId}/update-reaction-count")
    public ResponseEntity<Void> updatePostReactionCountInternal(
            @PathVariable Long postId,
            @RequestBody ReactionCountUpdateDTO countDTO) {
        try {
            postService.updatePostReactionCount(postId, countDTO.getReactionCount());
            return ResponseEntity.ok().build();
        } catch (UserException e) {
            log.error("Failed to update reaction count for post {}: {}", postId, e.getMessage());
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Unexpected error updating reaction count for post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUser(@PathVariable Long userId) {
        try {
            List<PostViewDTO> posts = postService.getPostsByUser(userId);
            return ResponseEntity.ok(posts);
        } catch (UserException e) {
            log.warn("Could not get posts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving posts for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving posts.");
        }
    }

    @GetMapping("/hashtag/{hashtag}")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtag(
            @PathVariable String hashtag) {
        List<PostViewDTO> posts = postService.getPostsByHashtag(hashtag);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/hashtags")
    public ResponseEntity<List<PostViewDTO>> getPostsByHashtags(
            @RequestParam(required = false) List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<PostViewDTO> posts = postService.getPostsByHashtags(tags);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostViewDTO>> searchPostsByText(
            @RequestParam String query) {
        List<PostViewDTO> posts = postService.searchPostsByText(query);
        return ResponseEntity.ok(posts);
    }
}
