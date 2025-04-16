package com.example.demo.builder.commentbuilder;

import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.time.LocalDateTime;

@Component
public class CommentBuilder {
    public Comment generateEntityFromDTO(CommentDTO commentDTO, User user, Post post) {
        Comment comment = new Comment();
        comment.setContent(commentDTO.getContent());
        comment.setUser(user);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());

        if (commentDTO.getImageBase64() != null && !commentDTO.getImageBase64().isEmpty()) {
            comment.setImage(Base64.getDecoder().decode(commentDTO.getImageBase64()));
        }

        return comment;
    }
}