package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class CommentBuilder {

    private static final Logger log = LoggerFactory.getLogger(CommentBuilder.class);

    public Comment generateEntityFromDTO(CommentDTO commentDTO, User user, Post post) {
        Comment comment = new Comment();
        comment.setContent(commentDTO.getContent());
        comment.setUser(user);
        comment.setPost(post);

        String imageBase64 = commentDTO.getImageBase64();
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            try {
                comment.setImage(Base64.getDecoder().decode(imageBase64));
                log.debug("Image bytes set for new comment");
            } catch (IllegalArgumentException e) {
                log.error("Invalid Base64 image data received for new comment: {}", e.getMessage());
                comment.setImage(null);
            }
        } else {
            comment.setImage(null);
        }

        return comment;
    }
}