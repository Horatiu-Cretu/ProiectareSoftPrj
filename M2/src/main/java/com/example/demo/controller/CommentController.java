package com.example.demo.controller;

import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/post/{postId}")
    public Mono<ResponseEntity<CommentViewDTO>> createComment(
            @PathVariable Long postId,
            @RequestBody CommentDTO commentDTO,
            @RequestHeader("Authorization") String authHeader) {
        return Mono.fromCallable(() -> {
            Long userId = extractUserIdFromToken(authHeader);
            return ResponseEntity.ok(commentService.createComment(postId, commentDTO, userId));
        }).onErrorMap(e -> new RuntimeException("Error creating comment: " + e.getMessage()));
    }

    @PutMapping("/{commentId}")
    public Mono<ResponseEntity<CommentViewDTO>> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDTO commentDTO,
            @RequestHeader("Authorization") String authHeader) {
        return Mono.fromCallable(() -> {
            Long userId = extractUserIdFromToken(authHeader);
            return ResponseEntity.ok(commentService.updateComment(commentId, commentDTO, userId));
        }).onErrorMap(e -> new RuntimeException("Error updating comment: " + e.getMessage()));
    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authHeader) {
        return Mono.fromCallable(() -> {
            Long userId = extractUserIdFromToken(authHeader);
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().<Void>build();
        }).onErrorMap(e -> new RuntimeException("Error deleting comment: " + e.getMessage()));
    }

    @GetMapping("/post/{postId}")
    public Mono<ResponseEntity<String>> getFormattedCommentsByPost(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader) {
        return Mono.just(ResponseEntity.ok(commentService.getFormattedCommentsByPost(postId)));
    }

    private Long extractUserIdFromToken(String authHeader) {
        // Implement token validation and user ID extraction
        // This should use your security configuration to validate the JWT token
        // and extract the user ID from it
        return 0L; // Replace with actual implementation
    }
}