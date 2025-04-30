package com.example.demo.dto.postdto;

import com.example.demo.entity.PostType;
import jakarta.validation.constraints.NotNull; // Example validation if needed
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

// Import List
import java.util.List;
// Keep Set import if used elsewhere, or remove
// import java.util.Set;

@Data
public class PostDTO {

    // Consider adding validation if needed (e.g., @NotEmpty)
    private String content;

    private MultipartFile image; // For uploads

    @NotNull(message = "Post type cannot be null") // Example validation
    private PostType postType;

    // Change from Set to List for better data binding from multipart/form-data
    private List<String> hashtags;
}