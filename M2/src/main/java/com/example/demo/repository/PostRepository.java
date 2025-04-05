package com.example.demo.repository;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Post> findAllByOrderByCreatedAtDesc();
    void deleteByUserIdAndId(Long userId, Long postId);
    boolean existsByUserIdAndId(Long userId, Long postId);
    Post findByUserIdAndId(Long userId, Long postId);
    List<Post> findAllByHashtags_NameIgnoreCaseOrderByCreatedAtDesc(String hashtag);
    List<Post> findAllByUserIdAndPostTypeOrderByCreatedAtDesc(Long userId, String postType);
    List<Post> findAllByUserIdAndPostTypeAndHashtags_NameIgnoreCaseOrderByCreatedAtDesc(Long userId, String postType, String hashtag);
    List<Post> findAllByUserIdAndHashtags_NameIgnoreCaseOrderByCreatedAtDesc(Long userId, String hashtag);

    Collection<Object> findAllByUserOrderByCreatedAtDesc(User user);

    Collection<Object> findAllByHashtags_NameInIgnoreCaseOrderByCreatedAtDesc(List<String> hashtags);
}