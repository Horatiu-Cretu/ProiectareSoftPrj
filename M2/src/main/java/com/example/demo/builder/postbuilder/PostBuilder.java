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

    public Post generateEntityFromDTO(PostDTO postDTO, User user) throws IOException {
        Post post = new Post();
        post.setContent(postDTO.getContent());
        post.setPostType(postDTO.getPostType());
        post.setUser(user);

        MultipartFile imageFile = postDTO.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                post.setImage(imageFile.getBytes());
                log.debug("Image bytes set for new post");
            } catch (IOException e) {
                log.error("IOException reading image bytes: {}", e.getMessage());
                throw e;
            }
        }


        return post;
    }
}