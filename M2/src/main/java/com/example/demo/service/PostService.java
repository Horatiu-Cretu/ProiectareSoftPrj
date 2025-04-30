package com.example.demo.service;

import com.example.demo.builder.postbuilder.PostBuilder;
import com.example.demo.builder.postbuilder.PostViewBuilder;
import com.example.demo.dto.postdto.PostDTO;
import com.example.demo.dto.postdto.PostViewDTO;
import com.example.demo.entity.Hashtag;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.HashtagRepository;
import com.example.demo.repository.PostRepository;
import jakarta.transaction.Transactional; // Use jakarta.transaction.Transactional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection; // Import Collection
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

    // Constructor Injection
    public PostService(PostRepository postRepository,
                       UserService userService,
                       PostBuilder postBuilder,
                       HashtagRepository hashtagRepository) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.postBuilder = postBuilder;
        this.hashtagRepository = hashtagRepository;
    }

    @Transactional
    public PostViewDTO createPost(PostDTO postDTO, Long userId) throws UserException {
        User user = userService.getUserById(userId); // Fetch the user entity

        Post post;
        try {
            // PostBuilder creates the basic Post entity from DTO fields (content, type, image, user)
            post = postBuilder.generateEntityFromDTO(postDTO, user);

            // Service layer handles finding/creating and associating Hashtag entities
            // Pass the List<String> from the DTO to the helper method
            Set<Hashtag> managedHashtags = findOrCreateHashtags(postDTO.getHashtags());

            // Associate the managed Hashtag entities with the Post
            // Clear any potential defaults and add the processed ones
            post.getHashtags().clear();
            if (managedHashtags != null && !managedHashtags.isEmpty()) {
                managedHashtags.forEach(tag -> post.addHashtag(tag)); // Use Post entity's helper method
                // Alternatively: post.setHashtags(managedHashtags); but helper is safer
            }

        } catch (IOException e) {
            log.error("Error processing image during post creation for user {}: {}", userId, e.getMessage());
            throw new UserException("Error processing image file.", e);
        } catch (Exception e) {
            log.error("Unexpected error during post creation logic for user {}: {}", userId, e.getMessage(), e);
            throw new UserException("An unexpected error occurred while creating the post.", e);
        }

        log.info("Attempting to save post for user {}", userId);
        Post savedPost = postRepository.save(post);
        log.info("Post created with ID {}", savedPost.getId());

        // Eagerly load necessary associations for the view DTO *if needed*
        // This might not be required if your fetch types and ViewBuilder are set up correctly
        // Hibernate.initialize(savedPost.getHashtags());
        // Hibernate.initialize(savedPost.getComments());

        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    @Transactional
    public PostViewDTO updatePost(Long postId, PostDTO postDTO, Long userId) throws UserException {
        log.info("Attempting to update post {} by user {}", postId, userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId));

        // --- Ownership Check ---
        if (!post.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to update post {} owned by {}", userId, postId, post.getUser().getId());
            throw new UserException("Not authorized to update this post");
        }

        // Update mutable fields from DTO
        post.setContent(postDTO.getContent());
        post.setPostType(postDTO.getPostType());

        // Handle image update
        MultipartFile imageFile = postDTO.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                log.debug("Updating image for post {}", postId);
                post.setImage(imageFile.getBytes());
            } catch (IOException e) {
                log.error("Error processing image update for post {}: {}", postId, e.getMessage());
                throw new UserException("Error processing updated image file.", e);
            }
        } else {
            // Logic if you want to allow image removal via update:
            // if (postDTO.getRemoveImage() != null && postDTO.getRemoveImage()) { // Assuming a boolean field in DTO
            //    post.setImage(null);
            // }
        }

        // --- Handle hashtag update ---
        // 1. Find or create Hashtag entities based on the new list from the DTO
        Set<Hashtag> updatedHashtags = findOrCreateHashtags(postDTO.getHashtags()); // Pass the List

        // 2. Clear the existing associations from the Post side
        //    Use the helper method in Post entity to ensure bidirectional consistency
        //    Create a copy to avoid ConcurrentModificationException while iterating
        Set<Hashtag> currentTags = new HashSet<>(post.getHashtags());
        currentTags.forEach(tag -> post.removeHashtag(tag));
        // Or if no helper: post.getHashtags().clear(); but less safe for bidirectional

        // 3. Add the new associations from the Post side
        if (updatedHashtags != null && !updatedHashtags.isEmpty()) {
            updatedHashtags.forEach(tag -> post.addHashtag(tag)); // Use helper method
        }
        // --- End hashtag update ---

        // updatedAt timestamp is handled automatically by @PreUpdate in the Post entity
        Post savedPost = postRepository.save(post);
        log.info("Post {} updated successfully by user {}", postId, userId);

        // Eagerly load if needed for ViewBuilder
        // Hibernate.initialize(savedPost.getHashtags());
        // Hibernate.initialize(savedPost.getComments());

        return PostViewBuilder.generateDTOFromEntity(savedPost);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) throws UserException {
        log.info("Attempting to delete post {} by user {}", postId, userId);
        // Fetch post first to check ownership *before* delete call
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to delete post {} owned by {}", userId, postId, post.getUser().getId());
            throw new UserException("Not authorized to delete this post");
        }

        // If ownership is confirmed, delete the post. Cascade should handle comments.
        // ManyToMany with Hashtag might need explicit handling if Hashtags shouldn't be deleted.
        // The current CascadeType on Post.hashtags (PERSIST, MERGE) is correct - it won't delete Hashtags.
        postRepository.delete(post);
        log.info("Post {} deleted successfully by user {}", postId, userId);
    }


    // --- Retrieval Methods (No changes needed here for the fix) ---

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostViewDTO> getAllPosts() {
        log.debug("Fetching all posts");
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostViewDTO> getPostsByUser(Long userId) throws UserException {
        log.debug("Fetching posts for user {}", userId);
        if (!userService.userExists(userId)) {
            throw new UserException("User not found with id: " + userId);
        }
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostViewDTO> getPostsByHashtag(String hashtag) {
        log.debug("Fetching posts for hashtag '{}'", hashtag);
        String normalizedTag = normalizeHashtag(hashtag);
        if (normalizedTag == null || normalizedTag.length() <= 1) return List.of(); // Handle invalid input

        return postRepository.findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(normalizedTag)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostViewDTO> getPostsByHashtags(List<String> hashtags) {
        log.debug("Fetching posts for hashtags: {}", hashtags);
        if (hashtags == null || hashtags.isEmpty()) {
            return List.of();
        }
        List<String> normalizedTags = hashtags.stream()
                .map(this::normalizeHashtag)
                .filter(tag -> tag != null && tag.length() > 1 && tag.length() <= 101) // Filter invalid/long tags (# + 100 chars)
                .map(String::toLowerCase)
                .distinct() // Avoid duplicate normalized tags in query
                .collect(Collectors.toList());

        if (normalizedTags.isEmpty()) {
            return List.of();
        }

        return postRepository.findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(normalizedTags)
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostViewDTO> searchPostsByText(String query) {
        log.debug("Searching posts by text query: '{}'", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(query.trim())
                .stream()
                .map(PostViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }


    // --- Helper Methods ---

    /**
     * Finds existing Hashtag entities or creates new ones for the given tag names.
     * Accepts a Collection (List or Set) of strings.
     * Returns a Set of managed Hashtag entities.
     */
    private Set<Hashtag> findOrCreateHashtags(Collection<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            log.debug("No tag names provided for findOrCreateHashtags.");
            return new HashSet<>(); // Return empty set
        }
        Set<Hashtag> managedTags = new HashSet<>();
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) {
                log.trace("Skipping empty or null tag name.");
                continue;
            }

            String normalizedName = normalizeHashtag(tagName.trim());
            // Add check for valid hashtag format/length *before* hitting DB
            if (normalizedName == null || normalizedName.length() <= 1 || normalizedName.length() > 101) { // # + 100 chars
                log.warn("Skipping invalid or too long normalized hashtag: '{}' (from input: '{}')", normalizedName, tagName);
                continue;
            }

            // Try to find existing tag (case-insensitive)
            Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> { // If not found, create it
                        log.info("Creating new hashtag: {}", normalizedName);
                        try {
                            // Create and save the new Hashtag entity
                            return hashtagRepository.save(new Hashtag(normalizedName));
                        } catch (Exception e) {
                            // Handle potential constraint violations (e.g., unique key)
                            // if another request created it concurrently between find and save.
                            log.error("Error saving new hashtag '{}': {}. Trying to find again.", normalizedName, e.getMessage());
                            // Attempt to find it again after the error
                            return hashtagRepository.findByNameIgnoreCase(normalizedName).orElse(null);
                        }
                    });

            if (hashtag != null) { // Add the found or newly created tag to the set
                managedTags.add(hashtag);
            } else {
                log.warn("Could not find or create hashtag for normalized name: {}", normalizedName);
            }
        }
        log.debug("Finished findOrCreateHashtags. Managed tags: {}", managedTags.stream().map(Hashtag::getName).collect(Collectors.toList()));
        return managedTags;
    }

    /**
     * Normalizes a hashtag string: trims whitespace, removes leading '#', adds single leading '#'.
     * Returns null if input is null.
     */
    private String normalizeHashtag(String tagName) {
        if (tagName == null) return null;
        tagName = tagName.trim();
        if (tagName.isEmpty()) return "#"; // Or maybe null/empty string depending on desired handling
        // Remove all leading '#' characters, then add one back.
        tagName = tagName.replaceAll("^#+", "");
        return "#" + tagName;
    }
}