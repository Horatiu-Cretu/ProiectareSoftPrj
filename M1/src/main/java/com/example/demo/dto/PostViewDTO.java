package com.example.demo.dto;

// Make sure the package for PostType matches where you put it in M1
import com.example.demo.entity.PostType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet; // Import HashSet

@Data
@NoArgsConstructor // Needed for Jackson deserialization
@AllArgsConstructor
public class PostViewDTO {
    private Long id;
    private String content;
    private String imageBase64; // Expect image as Base64 from M2
    private PostType postType; // Use the PostType enum defined in M1

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // Add updatedAt to match M2's DTO

    private Long userId;
    private Set<String> hashtags = new HashSet<>(); // Initialize to avoid nulls

    // Ensure M1 uses its own CommentViewDTO definition here
    private Set<CommentViewDTO> comments = new HashSet<>(); // Initialize to avoid nulls
}