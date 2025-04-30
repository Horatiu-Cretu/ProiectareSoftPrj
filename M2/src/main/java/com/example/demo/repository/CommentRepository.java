package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find comments for a specific post, ordered desc
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    // Find comments by a specific user, ordered desc (if needed)
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Delete a comment by its ID and the owner's User ID
    // Use @Modifying and @Transactional on the service method or here if needed
    void deleteByIdAndUserId(Long id, Long userId);

    // Check if a comment exists with a given ID and owner User ID
    boolean existsByIdAndUserId(Long id, Long userId);
}