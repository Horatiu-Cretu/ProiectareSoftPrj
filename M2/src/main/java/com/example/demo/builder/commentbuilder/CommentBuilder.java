package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging
import org.springframework.stereotype.Component;

import java.util.Base64;
// Removed unused LocalDateTime import

@Component
public class CommentBuilder {

    private static final Logger log = LoggerFactory.getLogger(CommentBuilder.class);

    public Comment generateEntityFromDTO(CommentDTO commentDTO, User user, Post post) {
        Comment comment = new Comment();
        comment.setContent(commentDTO.getContent());
        comment.setUser(user);
        comment.setPost(post);
        // createdAt will be set by @PrePersist

        // Handle optional image from Base64
        String imageBase64 = commentDTO.getImageBase64();
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            try {
                comment.setImage(Base64.getDecoder().decode(imageBase64));
                log.debug("Image bytes set for new comment");
            } catch (IllegalArgumentException e) {
                // Handle invalid Base64 data gracefully
                log.error("Invalid Base64 image data received for new comment: {}", e.getMessage());
                // Decide: throw exception, log and skip, or set image to null?
                // For now, log and skip setting the image:
                comment.setImage(null);
                // Optionally: throw new RuntimeException("Invalid image data format.");
            }
        } else {
            comment.setImage(null); // Ensure image is null if not provided or empty
        }

        return comment;
    }
}