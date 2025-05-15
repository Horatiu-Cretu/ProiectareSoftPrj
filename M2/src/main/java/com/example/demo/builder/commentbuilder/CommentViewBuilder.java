package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setReactionCount(comment.getReactionCount()); // Adaugat

        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
        } else {
            log.warn("Comment entity {} has null User", comment.getId());
            dto.setUserId(null);
        }

        if (comment.getPost() != null) {
            dto.setPostId(comment.getPost().getId());
        } else {
            log.warn("Comment entity {} has null Post", comment.getId());
            dto.setPostId(null);
        }

        if (comment.getImage() != null && comment.getImage().length > 0) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(comment.getImage()));
        } else {
            dto.setImageBase64(null);
        }

        return dto;
    }
}