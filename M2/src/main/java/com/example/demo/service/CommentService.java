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
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
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

    public CommentViewDTO createComment(Long postId, CommentDTO commentDTO, Long userId) throws UserException {
        User user = userService.getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserException("Post not found"));

        Comment comment = commentBuilder.generateEntityFromDTO(commentDTO, user, post);
        Comment savedComment = commentRepository.save(comment);
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    public CommentViewDTO updateComment(Long commentId, CommentDTO commentDTO, Long userId) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to update this comment");
        }

        comment.setContent(commentDTO.getContent());
        if (commentDTO.getImageBase64() != null && !commentDTO.getImageBase64().isEmpty()) {
            comment.setImage(java.util.Base64.getDecoder().decode(commentDTO.getImageBase64()));
        }
        comment.setUpdatedAt(java.time.LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return CommentViewBuilder.generateDTOFromEntity(savedComment);
    }

    public void deleteComment(Long commentId, Long userId) throws UserException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UserException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new UserException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    public List<CommentViewDTO> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(CommentViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    public String getFormattedCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        StringBuilder result = new StringBuilder();

        for (Comment comment : comments) {
            result.append("Comment ID: ").append(comment.getId())
                  .append("\nContent: ").append(comment.getContent())
                  .append("\nAuthor ID: ").append(comment.getUser().getId())
                  .append("\nCreated At: ").append(comment.getCreatedAt())
                  .append("\n-------------------\n");
        }

        return result.toString();
    }
}