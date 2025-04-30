package com.example.demo.controller;

import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid; // For DTO validation
import org.slf4j.Logger; // Add Logging
import org.slf4j.LoggerFactory; // Add Logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // For param/path validation
import org.springframework.web.bind.annotation.*;

import java.util.List; // Import List

@RestController
@RequestMapping("/api/comments")
@Validated
public class CommentController extends BaseController { // Inherit getCurrentUserId

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        super(); // Call BaseController constructor
        this.commentService = commentService;
    }

    // --- Create Comment ---
    @PostMapping("/post/{postId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO, // Validate DTO
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("Received request from user {} to create comment on post {}", userId, postId);
            CommentViewDTO createdComment = commentService.createComment(postId, commentDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
        } catch (UserException e) {
            log.warn("UserException during comment creation on post {}: {}", postId, e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default
            String lowerCaseMsg = e.getMessage().toLowerCase();
            if (lowerCaseMsg.contains("post not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lowerCaseMsg.contains("user not found")) {
                status = HttpStatus.UNAUTHORIZED; // Or NOT_FOUND depending on context
            } else if (lowerCaseMsg.contains("authorized")) {
                status = HttpStatus.UNAUTHORIZED;
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating comment on post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating comment: " + e.getMessage());
        }
    }

    // --- Update Comment (Optional - if needed) ---
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDTO commentDTO, // Validate DTO
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("Received request from user {} to update comment {}", userId, commentId);
            CommentViewDTO updatedComment = commentService.updateComment(commentId, commentDTO, userId);
            return ResponseEntity.ok(updatedComment);
        } catch (UserException e) {
            log.warn("UserException during comment update for comment {}: {}", commentId, e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default
            String lowerCaseMsg = e.getMessage().toLowerCase();
            if (lowerCaseMsg.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lowerCaseMsg.contains("not authorized")) {
                status = HttpStatus.FORBIDDEN; // Use 403 Forbidden
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating comment: " + e.getMessage());
        }
    }


    // --- Delete Comment ---
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("Received request from user {} to delete comment {}", userId, commentId);
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (UserException e) {
            log.warn("UserException during comment deletion for comment {}: {}", commentId, e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default
            String lowerCaseMsg = e.getMessage().toLowerCase();
            if (lowerCaseMsg.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lowerCaseMsg.contains("not authorized")) {
                status = HttpStatus.FORBIDDEN; // Use 403 Forbidden
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment: " + e.getMessage());
        }
    }

    // --- Get Comments for Post (Public - using ViewDTO) ---
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getCommentsByPost(
            @PathVariable Long postId) {
        log.debug("Received request to get comments for post {}", postId);
        try {
            // Assuming public access
            List<CommentViewDTO> comments = commentService.getCommentsByPost(postId);
            return ResponseEntity.ok(comments);
        } catch (UserException e) {
            // Handle case where post doesn't exist
            log.warn("UserException fetching comments for post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching comments for post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching comments: " + e.getMessage());
        }
    }

    // --- Get Comments Formatted (Public - keeps original string format if needed) ---
    // Consider removing this if getCommentsByPost (returning DTOs) is sufficient
    @GetMapping("/post/{postId}/formatted")
    public ResponseEntity<String> getFormattedCommentsByPost(
            @PathVariable Long postId) {
        log.debug("Received request to get formatted comments for post {}", postId);
        try {
            // Assuming public access
            String formattedComments = commentService.getFormattedCommentsByPost(postId);
            return ResponseEntity.ok(formattedComments);
        } catch (UserException e) {
            log.warn("UserException fetching formatted comments for post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching formatted comments for post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching formatted comments: " + e.getMessage());
        }
    }
}