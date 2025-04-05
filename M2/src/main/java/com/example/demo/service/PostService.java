package com.example.demo.service;

import com.example.demo.builder.postbuilder.PostBuilder;
import com.example.demo.builder.postbuilder.PostViewBuilder;
import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final PostBuilder postBuilder;

    public PostService(PostRepository postRepository, UserService userService, PostBuilder postBuilder) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.postBuilder = postBuilder;
    }

    public PostViewDTO createPost(PostDTO postDTO, Long userId) throws IOException, UserException {
        User user = userService.getUserById(userId);
        Post post = postBuilder.generateEntityFromDTO(postDTO, user);
        Post savedPost = postRepository.save(post);
        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    public PostViewDTO updatePost(Long postId, PostDTO postDTO, Long userId) throws IOException, UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to update this post");
        }

        Post updatedPost = postBuilder.generateEntityFromDTO(postDTO, post.getUser());
        updatedPost.setId(postId);
        Post savedPost = postRepository.save(updatedPost);
        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    public List<PostViewDTO> getPostsByUser(Long userId) throws UserException {
        User user = userService.getUserById(userId);
        return postRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(post -> PostViewBuilder.generateDTOFromEntity((Post) post))
                .collect(Collectors.toList());
    }

    public List<PostViewDTO> getPostsByHashtag(String hashtag) {
        return postRepository.findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(hashtag)
                .stream()
                .map(post -> PostViewBuilder.generateDTOFromEntity((Post) post))
                .collect(Collectors.toList());
    }

    public List<PostViewDTO> getPostsByHashtags(List<String> hashtags) {
        return postRepository.findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(hashtags)
                .stream()
                .map(post -> PostViewBuilder.generateDTOFromEntity((Post) post))
                .collect(Collectors.toList());
    }

    public List<PostViewDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }
}