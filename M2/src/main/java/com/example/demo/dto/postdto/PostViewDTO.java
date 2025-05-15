package com.example.demo.dto.postdto;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.PostType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostViewDTO {
    private Long id;
    private String content;
    private String imageBase64;
    private PostType postType;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long userId;
    private int reactionCount;
    private Set<String> hashtags = new HashSet<>();
    private Set<CommentViewDTO> comments = new HashSet<>();
}