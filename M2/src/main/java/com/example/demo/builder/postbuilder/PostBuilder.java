package com.example.demo.builder.postbuilder;

        import com.example.demo.dto.postdto.PostDTO;
        import com.example.demo.entity.Post;
        import com.example.demo.entity.User;
        import com.example.demo.entity.Hashtag;
        import com.example.demo.repository.HashtagRepository;
        import org.springframework.stereotype.Component;

        import java.io.IOException;
        import java.time.LocalDateTime;
        import java.util.HashSet;
        import java.util.Set;

        @Component
        public class PostBuilder {
            private final HashtagRepository hashtagRepository;

            public PostBuilder(HashtagRepository hashtagRepository) {
                this.hashtagRepository = hashtagRepository;
            }

            public Post generateEntityFromDTO(PostDTO postDTO, User user) throws IOException {
                Post post = new Post();
                post.setContent(postDTO.getContent());
                post.setPostType(postDTO.getPostType());
                post.setUser(user);
                post.setCreatedAt(LocalDateTime.now());

                if (postDTO.getImage() != null) {
                    post.setImage(postDTO.getImage().getBytes());
                }

                Set<Hashtag> hashtags = new HashSet<>();
                if (postDTO.getHashtags() != null) {
                    for (String tagName : postDTO.getHashtags()) {
                        String normalizedName = tagName.startsWith("#") ? tagName : "#" + tagName;
                        Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(normalizedName)
                            .orElseGet(() -> {
                                Hashtag newTag = new Hashtag();
                                newTag.setName(normalizedName);
                                return hashtagRepository.save(newTag);
                            });
                        hashtags.add(hashtag);
                    }
                }
                post.setHashtags(hashtags);

                return post;
            }
        }