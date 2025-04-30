package com.example.demo.repository;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection; // Keep Collection if needed, otherwise use List
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Find posts by specific user, ordered by creation time descending
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find all posts, ordered by creation time descending
    List<Post> findAllByOrderByCreatedAtDesc();

    // Delete a post by its ID and the owner's User ID (for ownership check)
    // Use @Modifying and @Transactional on the service method or here if needed
    void deleteByIdAndUserId(Long id, Long userId);

    // Check if a post exists with a given ID and owner User ID
    boolean existsByIdAndUserId(Long id, Long userId);

    // Find posts by a single hashtag name (case-insensitive), ordered desc
    List<Post> findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(String hashtag);

    // Find posts containing ANY of the given hashtag names (case-insensitive), ordered desc
    @Query("SELECT DISTINCT p FROM Post p JOIN p.hashtags h WHERE LOWER(h.name) IN :hashtags ORDER BY p.createdAt DESC")
    List<Post> findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(@Param("hashtags") List<String> hashtags);

    // Find posts by User object (alternative to userId)
    List<Post> findAllByUserOrderByCreatedAtDesc(User user);

    // Basic text search (case-insensitive) - Use dedicated search for performance
    List<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String text);

    // Keep original methods if they are used elsewhere, but prefer List<Post> over Collection<Object>
    // Collection<Object> findAllByUserOrderByCreatedAtDesc(User user); // Original - replaced above
    // Collection<Object> findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(List<String> hashtags); // Original - replaced above
}