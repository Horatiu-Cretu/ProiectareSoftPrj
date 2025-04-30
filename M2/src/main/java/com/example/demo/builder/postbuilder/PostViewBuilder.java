package com.example.demo.builder.postbuilder;

import com.example.demo.builder.commentbuilder.CommentViewBuilder;
import com.example.demo.dto.commentdto.CommentViewDTO; // Import CommentViewDTO
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Comment; // Import Comment
import com.example.demo.entity.Post;
import com.example.demo.entity.Hashtag;
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging

import java.util.Base64;
import java.util.Comparator; // For sorting comments
import java.util.HashSet; // Import HashSet
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
        dto.setUpdatedAt(post.getUpdatedAt()); // Added updatedAt

        // Safely get User ID
        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
        } else {
            log.warn("Post entity {} has null User", post.getId());
            // Handle null user case appropriately, maybe set userId to null or a default
            dto.setUserId(null);
        }


        // Handle Image (byte[] to Base64 String)
        if (post.getImage() != null && post.getImage().length > 0) {
            dto.setImageBase64(Base64.getEncoder().encodeToString(post.getImage()));
        } else {
            dto.setImageBase64(null); // Explicitly set to null if no image
        }

        // Handle Hashtags (Set<Hashtag> to Set<String>)
        if (post.getHashtags() != null) {
            dto.setHashtags(post.getHashtags().stream()
                    .map(Hashtag::getName) // Assumes Hashtag::getName exists and is correct
                    .collect(Collectors.toSet()));
        } else {
            dto.setHashtags(new HashSet<>()); // Initialize empty set if null
        }

        // Handle Comments (Set<Comment> to Set<CommentViewDTO>)
        if (post.getComments() != null) {
            // Ensure comments are sorted by creation date descending for display
            dto.setComments(post.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt).reversed()) // Sort comments
                    .map(CommentViewBuilder::generateDTOFromEntity) // Map using CommentViewBuilder
                    .collect(Collectors.toCollection(HashSet::new))); // Collect into HashSet
        } else {
            dto.setComments(new HashSet<>()); // Initialize empty set if null
        }

        return dto;
    }
}