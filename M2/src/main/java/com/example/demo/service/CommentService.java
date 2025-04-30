package com.example.demo.service;

import com.example.demo.builder.commentbuilder.CommentBuilder;
import com.example.demo.builder.commentbuilder.CommentViewBuilder;
import com.example.demo.dto.commentdto.CommentDTO;
import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import org.slf4j.Logger; // Add logging
import org.slf4j.LoggerFactory; // Add logging
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional; // Use jakarta.transaction.Transactional
import java.util.Base64; // For Base64 decoding
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PostRepository postRepository;
    private final CommentBuilder commentBuilder;

    public CommentService(CommentRepository commentRepository, UserService userService,
                          PostRepository postRepository, CommentBuilder commentBuilder) {
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.commentBuilder = commentBuilder;
    }

    @Transactional
    public CommentViewDTO createComment(Long postId, CommentDTO commentDTO, Long userId) throws UserException {
        log.info("User {} attempting to comment on post {}", userId, postId);
        User user = userService.getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("Post not found: {}", postId);
                    return new UserException("Post not found with id: " + postId);
                });

        // Use builder (ensure it handles optional Base64 image)
        Comment comment = commentBuilder.generateEntityFromDTO(commentDTO, user, post);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment {} created successfully on post {} by user {}", savedComment.getId(), postId, userId);
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    // Optional: Update Comment (User didn't explicitly ask, but code exists)
    @Transactional
    public CommentViewDTO updateComment(Long commentId, CommentDTO commentDTO, Long userId) throws UserException {
        log.info("User {} attempting to update comment {}", userId, commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId));

        // --- Ownership Check ---
        if (!comment.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to update comment {} owned by {}", userId, commentId, comment.getUser().getId());
            throw new UserException("Not authorized to update this comment");
        }

        comment.setContent(commentDTO.getContent());

        // Handle optional image update from Base64
        if (commentDTO.getImageBase64() != null) {
            if (commentDTO.getImageBase64().isEmpty()) {
                comment.setImage(null); // Clear image if empty string provided
                log.debug("Clearing image for comment {}", commentId);
            } else {
                try {
                    comment.setImage(Base64.getDecoder().decode(commentDTO.getImageBase64()));
                    log.debug("Updating image for comment {}", commentId);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid Base64 image data received for comment update {}: {}", commentId, e.getMessage());
                    throw new UserException("Invalid image data format.");
                }
            }
        }
        // updatedAt handled by @PreUpdate

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment {} updated successfully by user {}", commentId, userId);
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) throws UserException {
        log.info("User {} attempting to delete comment {}", userId, commentId);

        // Fetch comment first for ownership check
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId));

        // --- Ownership Check ---
        if (!comment.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to delete comment {} owned by {}", userId, commentId, comment.getUser().getId());
            throw new UserException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment); // Use delete(entity) or deleteById
        log.info("Comment {} deleted successfully by user {}", commentId, userId);
    }

    // Get comments for a specific post
    @Transactional(Transactional.TxType.SUPPORTS) // Read-only if possible
    public List<CommentViewDTO> getCommentsByPost(Long postId) throws UserException {
        log.debug("Fetching comments for post {}", postId);
        // Optional: Check if post exists first
        if (!postRepository.existsById(postId)) {
            throw new UserException("Post not found with id: " + postId);
        }
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(CommentViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    // Get comments formatted as string (as requested originally)
    @Transactional(Transactional.TxType.SUPPORTS)
    public String getFormattedCommentsByPost(Long postId) throws UserException {
        log.debug("Fetching formatted comments for post {}", postId);
        if (!postRepository.existsById(postId)) {
            throw new UserException("Post not found with id: " + postId);
        }
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        if (comments.isEmpty()) {
            return "No comments found for this post.";
        }

        StringBuilder result = new StringBuilder();
        for (Comment comment : comments) {
            result.append("Comment ID: ").append(comment.getId())
                    .append("\nAuthor ID: ").append(comment.getUser().getId()) // Assuming User entity is loaded or ID accessible
                    .append("\nContent: ").append(comment.getContent())
                    .append("\nCreated At: ").append(comment.getCreatedAt()) // Consider formatting the date/time
                    .append(comment.getImage() != null ? "\n(Image attached)" : "") // Indicate if image exists
                    .append("\n-------------------\n");
        }
        return result.toString();
    }
}