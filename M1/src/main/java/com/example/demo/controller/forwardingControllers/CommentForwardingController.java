package com.example.demo.controller.forwardingControllers;

import com.example.demo.dto.forwardingdto.CommentViewDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentForwardingController extends BaseForwardingController {

    public CommentForwardingController(@Value("${m2.service.url}") String m2ServiceUrl) {
        super(m2ServiceUrl, "/api/comments");
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentViewDTO> createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> commentRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return forwardPost("/post/" + postId, commentRequest, authHeader, CommentViewDTO.class);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentViewDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> commentRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return forwardPut("/" + commentId, commentRequest, authHeader, CommentViewDTO.class);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return forwardDelete("/" + commentId, authHeader);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentViewDTO>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return forwardGet("/post/" + postId, authHeader, new ParameterizedTypeReference<List<CommentViewDTO>>() {});
    }

}