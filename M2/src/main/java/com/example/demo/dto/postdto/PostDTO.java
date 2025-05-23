package com.example.demo.dto.postdto;

import com.example.demo.entity.PostType;
import jakarta.validation.constraints.NotNull; // Example validation if needed
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

// Import List
import java.util.List;


@Data
public class PostDTO {

    private String content;

    private MultipartFile image;

    @NotNull(message = "Post type cannot be null")
    private PostType postType;

    private List<String> hashtags;
}