package com.example.demo.dto.postdto;

import com.example.demo.entity.PostType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class PostDTO {
    private String content;
    private MultipartFile image;
    private PostType postType;
    private Set<String> hashtags;
}
