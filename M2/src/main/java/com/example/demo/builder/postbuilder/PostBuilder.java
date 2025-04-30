package com.example.demo.builder.postbuilder;

import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.entity.Hashtag;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.HashtagRepository; // Keep if hashtag logic is here
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile; // Add MultipartFile

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class PostBuilder {

    private static final Logger log = LoggerFactory.getLogger(PostBuilder.class);

    // Removed HashtagRepository injection - Service layer should handle hashtag logic
    // private final HashtagRepository hashtagRepository;
    // public PostBuilder(HashtagRepository hashtagRepository) {
    //     this.hashtagRepository = hashtagRepository;
    // }

    public Post generateEntityFromDTO(PostDTO postDTO, User user) throws IOException {
        Post post = new Post();
        post.setContent(postDTO.getContent());
        post.setPostType(postDTO.getPostType());
        post.setUser(user);
        // createdAt will be set by @PrePersist

        // Handle Image
        MultipartFile imageFile = postDTO.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                post.setImage(imageFile.getBytes());
                log.debug("Image bytes set for new post");
            } catch (IOException e) {
                log.error("IOException reading image bytes: {}", e.getMessage());
                throw e; // Re-throw exception to be handled by the service/controller
            }
        }

        // Hashtag logic moved to PostService.
        // The service will call findOrCreateHashtags and then set post.setHashtags()
        // Set<Hashtag> hashtags = new HashSet<>();
        // if (postDTO.getHashtags() != null) {
        //    ... logic to find/create hashtags ...
        // }
        // post.setHashtags(hashtags); // Service layer will handle this

        return post;
    }
}