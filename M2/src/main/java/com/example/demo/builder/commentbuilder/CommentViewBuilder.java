package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.Comment;
import java.util.Base64;

public class CommentViewBuilder {
    public static CommentViewDTO generateDTOFromEntity(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentViewDTO dto = new CommentViewDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setUserId(comment.getUser().getId());
        dto.setPostId(comment.getPost().getId());

        if (comment.getImage() != null) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(comment.getImage()));
        }

        return dto;
    }
}
