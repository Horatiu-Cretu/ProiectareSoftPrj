package com.example.demo.dto.postdto;

import com.example.demo.dto.commentdto.CommentViewDTO;
import com.example.demo.entity.PostType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostViewDTO {
    private Long id;
    private String content;
    private String imageBase64;
    private PostType postType;
    private LocalDateTime createdAt;
    private Long userId;
    private Set<String> hashtags;
    private Set<CommentViewDTO> comments;
}