package com.example.demo.service;

import com.example.demo.builder.postbuilder.PostBuilder;
import com.example.demo.builder.postbuilder.PostViewBuilder;
import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Hashtag;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.HashtagRepository;
import com.example.demo.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserService userService;
    private final PostBuilder postBuilder;
    private final HashtagRepository hashtagRepository;
    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;
    private final String m3BaseUrl;

    public PostService(PostRepository postRepository,
                       @Lazy UserService userService,
                       PostBuilder postBuilder,
                       HashtagRepository hashtagRepository,
                       CommentRepository commentRepository,
                       RestTemplate restTemplate,
                       @Value("${m3.service.url}") String m3BaseUrl) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.postBuilder = postBuilder;
        this.hashtagRepository = hashtagRepository;
        this.commentRepository = commentRepository;
        this.restTemplate = restTemplate;
        this.m3BaseUrl = m3BaseUrl;
    }

    @Transactional
    public PostViewDTO createPost(PostDTO postDTO, Long userId) throws UserException {
        User user = userService.getUserById(userId);
        Post post;
        try {
            post = postBuilder.generateEntityFromDTO(postDTO, user);
            Set<Hashtag> managedHashtags = findOrCreateHashtags(postDTO.getHashtags());
            post.getHashtags().clear();
            if (managedHashtags != null && !managedHashtags.isEmpty()) {
                managedHashtags.forEach(post::addHashtag);
            }
            post.setReactionCount(0);
        } catch (IOException e) {
            log.error("Error processing image during post creation for user {}: {}", userId, e.getMessage(), e);
            throw new UserException("Error processing image file.", e);
        } catch (Exception e) {
            log.error("Unexpected error during post creation logic for user {}: {}", userId, e.getMessage(), e);
            throw new UserException("An unexpected error occurred while creating the post.", e);
        }
        Post savedPost = postRepository.save(post);
        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    @Transactional
    public PostViewDTO updatePost(Long postId, PostDTO postDTO, Long userId) throws UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to update this post");
        }

        post.setContent(postDTO.getContent());
        post.setPostType(postDTO.getPostType());

        MultipartFile imageFile = postDTO.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                post.setImage(imageFile.getBytes());
            } catch (IOException e) {
                log.error("Error processing updated image file for post {}: {}", postId, e.getMessage(), e);
                throw new UserException("Error processing updated image file.", e);
            }
        } else if (postDTO.getImage() == null && post.getImage() != null) {
            post.setImage(null);
        }


        Set<Hashtag> updatedHashtags = findOrCreateHashtags(postDTO.getHashtags());
        post.getHashtags().clear();
        if (updatedHashtags != null) {
            updatedHashtags.forEach(post::addHashtag);
        }

        Post savedPost = postRepository.save(post);
        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) throws UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId));
        if (!post.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to delete this post");
        }
        postRepository.delete(post);
        log.info("User {} deleted post {}", userId, postId);

    }

    @Transactional
    public void deletePostAsAdmin(Long postId, Long adminUserId) throws UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId + " for admin deletion."));
        postRepository.delete(post);
        log.info("Admin {} deleted post {}. Associated comments and hashtags will be removed by cascade if configured, or M3 will clean reactions.", adminUserId, postId);
    }


    @Transactional(readOnly = true)
    public List<PostViewDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostViewDTO> getAllPostsOrderByReactionCountDesc() {
        return postRepository.findAllByOrderByReactionCountDescCreatedAtDesc()
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePostReactionCount(Long postId, int newDirectPostReactionCount) throws UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId + " when trying to update reaction count."));

        int commentReactionsSum = 0;
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        if (comments != null) {
            for (Comment comment : comments) {
                commentReactionsSum += comment.getReactionCount();
            }
        }

        post.setReactionCount(newDirectPostReactionCount + commentReactionsSum);
        log.info("Updating aggregate reaction count for post {}: direct (from M3)={}, comments={}, total={}",
                postId, newDirectPostReactionCount, commentReactionsSum, post.getReactionCount());
        postRepository.save(post);
    }

    @Transactional
    public void recalculateAndSaveAggregateReactionsForPost(Long postId) throws UserException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId + " for aggregate reaction recalculation."));

        int directPostReactionsFromM3 = 0;
        String m3PostReactionCountUrl = m3BaseUrl + "/api/m3/reactions/target/POST/" + postId + "/count";
        try {
            log.debug("Fetching direct reaction count for post {} from M3 URL: {}", postId, m3PostReactionCountUrl);
            ResponseEntity<Long> response = restTemplate.exchange(m3PostReactionCountUrl, HttpMethod.GET, null, Long.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                directPostReactionsFromM3 = response.getBody().intValue();
                log.debug("Successfully fetched direct reactions for post {}: {}", postId, directPostReactionsFromM3);
            } else {
                log.warn("Could not fetch direct reaction count for post {} from M3. Status: {}, Body: {}",
                        postId, response.getStatusCode(), response.getBody());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.info("Post {} not found in M3 (or no direct reactions - 404 received). Assuming 0 direct reactions.", postId);
                directPostReactionsFromM3 = 0;
            } else {
                log.error("Client error fetching direct reaction count for post {} from M3: {} - {}", postId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected error fetching direct reaction count for post {} from M3: {}", postId, e.getMessage(), e);
        }

        int commentReactionsSum = 0;
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        if (comments != null) {
            for (Comment comment : comments) {
                commentReactionsSum += comment.getReactionCount();
            }
        }
        log.debug("Sum of comment reactions for post {}: {}", postId, commentReactionsSum);

        post.setReactionCount(directPostReactionsFromM3 + commentReactionsSum);
        log.info("Recalculated aggregate reaction count for post {}: directM3={}, comments={}, total={}",
                postId, directPostReactionsFromM3, commentReactionsSum, post.getReactionCount());
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<PostViewDTO> getPostsByUser(Long userId) throws UserException {
        if (!userService.userExists(userId)) {
            throw new UserException("User not found with id: " + userId);
        }
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostViewDTO> getPostsByHashtag(String hashtagName) {
        String queryName = hashtagName.startsWith("#") ? hashtagName.substring(1) : hashtagName;
        if (queryName.trim().isEmpty()) return List.of();

        return postRepository.findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(queryName)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostViewDTO> getPostsByHashtags(List<String> hashtagNames) {
        if (hashtagNames == null || hashtagNames.isEmpty()) {
            return List.of();
        }
        List<String> normalizedQueryNames = hashtagNames.stream()
                .map(name -> name.startsWith("#") ? name.substring(1) : name)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedQueryNames.isEmpty()) {
            return List.of();
        }

        return postRepository.findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(normalizedQueryNames)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostViewDTO> searchPostsByText(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(query.trim())
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    private Set<Hashtag> findOrCreateHashtags(Collection<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        Set<Hashtag> managedTags = new HashSet<>();
        for (String tagNameInput : tagNames) {
            if (tagNameInput == null || tagNameInput.trim().isEmpty()) {
                continue;
            }
            String normalizedName = tagNameInput.trim().replaceAll("^#+", "");
            if (normalizedName.isEmpty() || normalizedName.length() > 100) {
                log.warn("Skipping invalid or too long hashtag: '{}'", tagNameInput);
                continue;
            }

            Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> {
                        try {
                            log.debug("Creating new hashtag with name: '{}'", normalizedName);
                            return hashtagRepository.save(new Hashtag(normalizedName));
                        } catch (Exception e) {
                            log.warn("Could not save new hashtag '{}', possibly due to concurrent creation or other DB constraint. Attempting to find again. Error: {}", normalizedName, e.getMessage());
                            return hashtagRepository.findByNameIgnoreCase(normalizedName).orElse(null);
                        }
                    });
            if (hashtag != null) {
                managedTags.add(hashtag);
            }
        }
        return managedTags;
    }
}
