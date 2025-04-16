package com.example.demo.dto.commentdto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentViewDTO {
    private Long id;
    private String content;
    private String imageBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Long postId;
}