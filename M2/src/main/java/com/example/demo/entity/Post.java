package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // Import ToString

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
@Data // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Ensure user is mandatory
    @ToString.Exclude // Avoid recursion in toString
    @EqualsAndHashCode.Exclude // Avoid recursion in equals/hashCode
    private User user;

    @Column(columnDefinition = "TEXT") // Use TEXT for potentially long content
    private String content;

    @Lob
    @Column(columnDefinition="LONGBLOB") // Explicitly define column type for large images
    private byte[] image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // Ensure post type is mandatory
    private PostType postType;

    @Column(nullable = false, updatable = false) // Ensure createdAt is mandatory and not updated
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // Default fetch type is LAZY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Comment> comments = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }) // Manage hashtags lifecycle slightly differently
    @JoinTable(
            name = "post_hashtags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // Break potential cycles
    private Set<Hashtag> hashtags = new HashSet<>();

    // Helper methods for managing relationships (optional but recommended)
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setPost(null);
    }

    public void addHashtag(Hashtag hashtag) {
        this.hashtags.add(hashtag);
        hashtag.getPosts().add(this);
    }

    public void removeHashtag(Hashtag hashtag) {
        this.hashtags.remove(hashtag);
        hashtag.getPosts().remove(this);
    }
    // Ensure clearHashtags only removes the association, not the hashtags themselves
    public void clearHashtags() {
        // Create a copy to avoid ConcurrentModificationException
        Set<Hashtag> currentTags = new HashSet<>(this.hashtags);
        for (Hashtag tag : currentTags) {
            removeHashtag(tag);
        }
    }
}