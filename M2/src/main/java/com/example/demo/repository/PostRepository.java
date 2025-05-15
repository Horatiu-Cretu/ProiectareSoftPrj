package com.example.demo.repository;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByOrderByReactionCountDescCreatedAtDesc();

    void deleteByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Post> findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(String hashtag);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.hashtags h WHERE LOWER(h.name) IN :hashtags ORDER BY p.createdAt DESC")
    List<Post> findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(@Param("hashtags") List<String> hashtags);

    List<Post> findAllByUserOrderByCreatedAtDesc(User user);

    List<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String text);
}