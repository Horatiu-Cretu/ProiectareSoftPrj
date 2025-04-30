package com.example.demo.dto.postdto;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.PostType;
import com.fasterxml.jackson.annotation.JsonFormat; // For consistent date formatting
import lombok.Data;
import lombok.NoArgsConstructor; // Add NoArgsConstructor
import lombok.AllArgsConstructor; // Add AllArgsConstructor

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet; // Import HashSet

@Data
@NoArgsConstructor // Needed for deserialization if used that way
@AllArgsConstructor // Optional convenience constructor
public class PostViewDTO {
    private Long id;
    private String content;
    private String imageBase64; // Send image as Base64 encoded string
    private PostType postType;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") // Standard format
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") // Standard format
    private LocalDateTime updatedAt; // Added updatedAt

    private Long userId;
    private Set<String> hashtags = new HashSet<>(); // Initialize to avoid nulls
    private Set<CommentViewDTO> comments = new HashSet<>(); // Initialize to avoid nulls
}