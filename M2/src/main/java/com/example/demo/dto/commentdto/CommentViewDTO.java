package com.example.demo.dto.commentdto;

import com.fasterxml.jackson.annotation.JsonFormat; // For consistent date formatting
import lombok.Data;
import lombok.NoArgsConstructor; // Add NoArgsConstructor
import lombok.AllArgsConstructor; // Add AllArgsConstructor

import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Needed for deserialization
@AllArgsConstructor // Optional convenience constructor
public class CommentViewDTO {
    private Long id;
    private String content;
    private String imageBase64; // Image as Base64

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // Keep updatedAt

    private Long userId;
    private Long postId;
}