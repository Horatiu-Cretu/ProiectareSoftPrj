package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor; // Add NoArgsConstructor
import lombok.AllArgsConstructor; // Add AllArgsConstructor
import lombok.ToString; // Import ToString
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor // Add default constructor for JPA
@AllArgsConstructor // Optional: Add constructor with all fields
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT") // Use TEXT
    private String content;

    @Lob
    @Column(columnDefinition="MEDIUMBLOB") // Use MEDIUMBLOB if images are smaller than posts
    private byte[] image;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Post post;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}