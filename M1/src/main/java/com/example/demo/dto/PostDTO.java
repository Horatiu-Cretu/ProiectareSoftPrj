package com.example.demo.dto;

// Use the PostType enum previously added to M1
import com.example.demo.entity.PostType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile; // Needed for file uploads

// Import List
import java.util.List;
// Keep Set import if used elsewhere, or remove
// import java.util.Set;

@Data // Lombok annotation for getters, setters, etc.
public class PostDTO {

    private String content;

    private MultipartFile image; // To receive potential file upload

    private PostType postType;

    // Change from Set to List for better data binding from multipart/form-data
    private List<String> hashtags;
}