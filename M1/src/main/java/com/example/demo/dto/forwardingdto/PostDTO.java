package com.example.demo.dto.forwardingdto;

import com.example.demo.entity.PostType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@Data
public class PostDTO {

    private String content;

    private MultipartFile image;

    private PostType postType;

    private List<String> hashtags;
}