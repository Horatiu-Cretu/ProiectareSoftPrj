package com.example.demo.builder.postbuilder;

import com.example.demo.builder.commentbuilder.CommentViewBuilder;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Post;
import java.util.Base64;
import java.util.stream.Collectors;
import com.example.demo.entity.Hashtag;


public class PostViewBuilder {
    public static PostViewDTO generateDTOFromEntity(Post post) {
        if (post == null) {
            return null;
        }

        PostViewDTO dto = new PostViewDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setPostType(post.getPostType());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUserId(post.getUser().getId());

        if (post.getImage() != null) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(post.getImage()));
        }

        if (post.getHashtags() != null) {
            dto.setHashtags(post.getHashtags().stream()
                    .map(Hashtag::getName)
                    .collect(Collectors.toSet()));
        }
        if (post.getComments() != null) {
            dto.setComments(post.getComments().stream()
                    .map(CommentViewBuilder::generateDTOFromEntity)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}