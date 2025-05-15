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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional; // Corectat importul
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PostRepository postRepository;
    private final CommentBuilder commentBuilder;
    private final PostService postService;

    public CommentService(CommentRepository commentRepository,
                          @Lazy UserService userService,
                          PostRepository postRepository,
                          CommentBuilder commentBuilder,
                          @Lazy PostService postService) {
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.commentBuilder = commentBuilder;
        this.postService = postService;
    }

    @Transactional
    public CommentViewDTO createComment(Long postId, CommentDTO commentDTO, Long userId) throws UserException {
        User user = userService.getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found with id: " + postId));
        Comment comment = commentBuilder.generateEntityFromDTO(commentDTO, user, post);
        comment.setReactionCount(0);
        Comment savedComment = commentRepository.save(comment);

        try {
            postService.recalculateAndSaveAggregateReactionsForPost(post.getId());
        } catch (UserException e) {
            log.error("Failed to trigger aggregate reaction update for post {} after new comment {} creation: {}",
                    post.getId(), savedComment.getId(), e.getMessage());
        }
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    @Transactional
    public CommentViewDTO updateComment(Long commentId, CommentDTO commentDTO, Long userId) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId));
        if (!comment.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to update this comment");
        }
        comment.setContent(commentDTO.getContent());
        if (commentDTO.getImageBase64() != null) {
            if (commentDTO.getImageBase64().isEmpty()) {
                comment.setImage(null);
            } else {
                try {
                    comment.setImage(Base64.getDecoder().decode(commentDTO.getImageBase64()));
                } catch (IllegalArgumentException e) {
                    throw new UserException("Invalid image data format.");
                }
            }
        }
        Comment savedComment = commentRepository.save(comment);
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId));
        if (!comment.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to delete this comment");
        }
        Long parentPostId = comment.getPost() != null ? comment.getPost().getId() : null;

        commentRepository.delete(comment);
        log.info("User {} deleted comment {}", userId, commentId);


        if (parentPostId != null) {
            try {
                postService.recalculateAndSaveAggregateReactionsForPost(parentPostId);
            } catch (UserException e) {
                log.error("Failed to trigger aggregate reaction update for post {} after comment {} deletion by user: {}",
                        parentPostId, commentId, e.getMessage());
            }
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CommentViewDTO> getCommentsByPost(Long postId) throws UserException {
        if (!postRepository.existsById(postId)) {
            throw new UserException("Post not found with id: " + postId);
        }
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(CommentViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateCommentReactionCount(Long commentId, int newCommentReactionCount) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId + " when trying to update reaction count."));

        comment.setReactionCount(newCommentReactionCount);
        Comment savedComment = commentRepository.save(comment);
        log.info("Updated reaction count for comment {} to {}", commentId, newCommentReactionCount);

        if (savedComment.getPost() != null) {
            try {
                log.debug("Triggering aggregate recalculation for post {} due to comment {} reaction update.",
                        savedComment.getPost().getId(), commentId);
                postService.recalculateAndSaveAggregateReactionsForPost(savedComment.getPost().getId());
            } catch (UserException e) {
                log.error("Failed to trigger aggregate reaction update for post {} after comment {} reaction update: {}",
                        savedComment.getPost().getId(), commentId, e.getMessage());
            }
        } else {
            log.warn("Comment {} does not have an associated post. Cannot trigger aggregate update for parent post.", commentId);
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public String getFormattedCommentsByPost(Long postId) throws UserException {
        if (!postRepository.existsById(postId)) {
            throw new UserException("Post not found with id: " + postId);
        }
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        if (comments.isEmpty()) {
            return "No comments found for this post.";
        }
        StringBuilder result = new StringBuilder();
        for (Comment commentEntity : comments) {
            result.append("Comment ID: ").append(commentEntity.getId())
                    .append("\nAuthor ID: ").append(commentEntity.getUser() != null ? commentEntity.getUser().getId() : "N/A")
                    .append("\nContent: ").append(commentEntity.getContent())
                    .append("\nCreated At: ").append(commentEntity.getCreatedAt())
                    .append("\nReactions: ").append(commentEntity.getReactionCount())
                    .append(commentEntity.getImage() != null && commentEntity.getImage().length > 0 ? "\n(Image attached)" : "")
                    .append("\n-------------------\n");
        }
        return result.toString();
    }

    @Transactional
    public void deleteCommentAsAdmin(Long commentId, Long adminUserId) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found with id: " + commentId));

        Long parentPostId = comment.getPost() != null ? comment.getPost().getId() : null;
        Long originalAuthorId = comment.getUser() != null ? comment.getUser().getId() : null;

        commentRepository.delete(comment);
        log.info("Admin {} deleted comment {} (original author ID: {})", adminUserId, commentId, originalAuthorId != null ? originalAuthorId : "N/A");

        if (parentPostId != null) {
            try {
                log.info("Triggering aggregate reaction update for post {} after admin deleted comment {}", parentPostId, commentId);
                postService.recalculateAndSaveAggregateReactionsForPost(parentPostId);
            } catch (UserException e) {
                log.error("Failed to trigger aggregate reaction update for post {} after admin deleted comment {}: {}",
                        parentPostId, commentId, e.getMessage());
            }
        }
    }
}