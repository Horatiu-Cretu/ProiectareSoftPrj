package com.example.demo.controller;

import com.example.demo.dto.ReactionCountUpdateDTO;
import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.CommentService;
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
@RequestMapping("/api/m2/comments")
@Validated
public class CommentController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        super();
        this.commentService = commentService;
    }

    private ResponseEntity<?> handleUserException(UserException e, String action) {
        log.error("Error {}: {}", action, e.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String lowerCaseMsg = e.getMessage().toLowerCase();
        if (lowerCaseMsg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (lowerCaseMsg.contains("not authorized") || lowerCaseMsg.contains("unauthorized")) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(e.getMessage());
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            CommentViewDTO createdComment = commentService.createComment(postId, commentDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
        } catch (UserException e) {
            return handleUserException(e, "creating comment for post " + postId + " by user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error creating comment for post {} by user {}: {}", postId, request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating comment: " + e.getMessage());
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDTO commentDTO,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            CommentViewDTO updatedComment = commentService.updateComment(commentId, commentDTO, userId);
            return ResponseEntity.ok(updatedComment);
        } catch (UserException e) {
            return handleUserException(e, "updating comment " + commentId + " by user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error updating comment {} by user {}: {}", commentId, request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating comment: " + e.getMessage());
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (UserException e) {
            return handleUserException(e, "deleting comment " + commentId + " by user " + request.getAttribute("userId"));
        } catch (Exception e) {
            log.error("Unexpected error deleting comment {} by user {}: {}", commentId, request.getAttribute("userId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/comments/{commentId}")
    public ResponseEntity<?> deleteCommentAsAdmin(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        try {
            Long adminUserId = getCurrentUserId(request);
            log.info("Admin {} attempting to delete comment {}", adminUserId, commentId);
            commentService.deleteCommentAsAdmin(commentId, adminUserId);
            return ResponseEntity.noContent().build();
        } catch (UserException e) {
            log.error("Admin {} error deleting comment {}: {}", request.getAttribute("userId"), commentId, e.getMessage());
            return handleUserException(e, "admin deleting comment " + commentId);
        } catch (Exception e) {
            log.error("Unexpected admin error deleting comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during admin deletion of comment: " + e.getMessage());
        }
    }


    @PutMapping("/{commentId}/update-reaction-count")
    public ResponseEntity<Void> updateCommentReactionCountInternal(
            @PathVariable Long commentId,
            @RequestBody ReactionCountUpdateDTO countDTO) {
        try {
            commentService.updateCommentReactionCount(commentId, countDTO.getReactionCount());
            return ResponseEntity.ok().build();
        } catch (UserException e) {
            log.error("Failed to update reaction count for comment {}: {}", commentId, e.getMessage());
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error updating reaction count for comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getCommentsByPost(
            @PathVariable Long postId) {
        try {
            List<CommentViewDTO> comments = commentService.getCommentsByPost(postId);
            return ResponseEntity.ok(comments);
        } catch (UserException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching comments for post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching comments: " + e.getMessage());
        }
    }

    @GetMapping("/post/{postId}/formatted")
    public ResponseEntity<String> getFormattedCommentsByPost(
            @PathVariable Long postId) {
        try {
            String formattedComments = commentService.getFormattedCommentsByPost(postId);
            return ResponseEntity.ok(formattedComments);
        } catch (UserException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching formatted comments for post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching formatted comments: " + e.getMessage());
        }
    }
}
