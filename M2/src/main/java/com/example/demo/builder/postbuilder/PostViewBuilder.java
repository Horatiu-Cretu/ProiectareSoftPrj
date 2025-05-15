package com.example.demo.builder.postbuilder;

import com.example.demo.builder.commentbuilder.CommentViewBuilder;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.Hashtag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PostViewBuilder {

    private static final Logger log = LoggerFactory.getLogger(PostViewBuilder.class);

    public static PostViewDTO generateDTOFromEntity(Post post) {
        if (post == null) {
            log.warn("Attempted to build PostViewDTO from null Post entity");
            return null;
        }

        PostViewDTO dto = new PostViewDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setPostType(post.getPostType());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setReactionCount(post.getReactionCount());

        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
        } else {
            log.warn("Post entity {} has null User", post.getId());
            dto.setUserId(null);
        }

        if (post.getImage() != null && post.getImage().length > 0) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(post.getImage()));
        } else {
            dto.setImageBase64(null);
        }

        if (post.getHashtags() != null) {
            dto.setHashtags(post.getHashtags().stream()
                    .map(Hashtag::getName)
                    .collect(Collectors.toSet()));
        } else {
            dto.setHashtags(new HashSet<>());
        }

        if (post.getComments() != null) {
            dto.setComments(post.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                    .map(CommentViewBuilder::generateDTOFromEntity)
                    .collect(Collectors.toCollection(HashSet::new)));
        } else {
            dto.setComments(new HashSet<>());
        }

        return dto;
    }
}