package com.example.demo.dto.commentdto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty; // Add validation
import jakarta.validation.constraints.Size; // Add validation

@Data
public class CommentDTO {
    @NotEmpty(message = "Comment content cannot be empty")
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String content;

    private String imageBase64;
}