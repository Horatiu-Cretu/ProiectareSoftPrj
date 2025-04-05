package com.example.demo.controller;

    import com.example.demo.dto.postdto.PostDTO;
    import com.example.demo.dto.postdto.PostViewDTO;
    import com.example.demo.errorhandler.UserException;
    import com.example.demo.service.PostService;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.io.IOException;
    import java.util.List;

    @RestController
    @RequestMapping("/api/posts")
    public class PostController {
        private final PostService postService;

        public PostController(PostService postService) {
            this.postService = postService;
        }

        @PostMapping
        public ResponseEntity<PostViewDTO> createPost(@RequestBody PostDTO postDTO,
                                                    @RequestHeader("userId") Long userId) throws IOException, UserException {
            PostViewDTO post = postService.createPost(postDTO, userId);
            return ResponseEntity.ok(post);
        }

        @PutMapping("/{postId}")
        public ResponseEntity<PostViewDTO> updatePost(@PathVariable Long postId,
                                                    @RequestBody PostDTO postDTO,
                                                    @RequestHeader("userId") Long userId) throws IOException, UserException {
            PostViewDTO post = postService.updatePost(postId, postDTO, userId);
            return ResponseEntity.ok(post);
        }

        @GetMapping
        public ResponseEntity<List<PostViewDTO>> getAllPosts() {
            List<PostViewDTO> posts = postService.getAllPosts();
            return ResponseEntity.ok(posts);
        }
        @GetMapping("/user/{userId}")
        public ResponseEntity<List<PostViewDTO>> getPostsByUser(@PathVariable Long userId) throws UserException {
            List<PostViewDTO> posts = postService.getPostsByUser(userId);
            return ResponseEntity.ok(posts);
        }

        @GetMapping("/hashtag/{hashtag}")
        public ResponseEntity<List<PostViewDTO>> getPostsByHashtag(@PathVariable String hashtag) {
            String normalizedHashtag = hashtag.startsWith("#") ? hashtag : "#" + hashtag;
            List<PostViewDTO> posts = postService.getPostsByHashtag(normalizedHashtag);
            return ResponseEntity.ok(posts);
        }

        @GetMapping("/hashtags")
        public ResponseEntity<List<PostViewDTO>> getPostsByHashtags(@RequestParam List<String> tags) {
            List<PostViewDTO> posts = postService.getPostsByHashtags(tags);
            return ResponseEntity.ok(posts);
        }
    }