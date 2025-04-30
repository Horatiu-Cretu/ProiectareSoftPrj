package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.Comment;
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging
import java.util.Base64;

public class CommentViewBuilder {

    private static final Logger log = LoggerFactory.getLogger(CommentViewBuilder.class);

    public static CommentViewDTO generateDTOFromEntity(Comment comment) {
        if (comment == null) {
            log.warn("Attempted to build CommentViewDTO from null Comment entity");
            return null;
        }

        CommentViewDTO dto = new CommentViewDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt()); // Added updatedAt

        // Safely get User ID
        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
        } else {
            log.warn("Comment entity {} has null User", comment.getId());
            dto.setUserId(null); // Handle null user
        }


        // Safely get Post ID
        if (comment.getPost() != null) {
            dto.setPostId(comment.getPost().getId());
        } else {
            log.warn("Comment entity {} has null Post", comment.getId());
            dto.setPostId(null); // Handle null post
        }


        // Handle Image (byte[] to Base64 String)
        if (comment.getImage() != null && comment.getImage().length > 0) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(comment.getImage()));
        } else {
            dto.setImageBase64(null); // Explicitly set to null if no image
        }

        return dto;
    }
}