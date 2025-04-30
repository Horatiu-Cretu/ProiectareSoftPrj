package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Needed for Jackson deserialization
@AllArgsConstructor
public class CommentViewDTO {
    private Long id;
    private String content;
    private String imageBase64; // Expect image as Base64 from M2

    // Ensure M1's Jackson can handle this format (default should be fine)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // Add updatedAt to match M2's DTO

    private Long userId;
    private Long postId;
}