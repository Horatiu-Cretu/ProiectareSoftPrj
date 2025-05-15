package com.example.demo.dto.reaction;



import com.example.demo.entity.ReactionType;
import com.example.demo.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionViewDTO {
    private Long id;
    private Long userId;
    private Long targetId;
    private TargetType targetType;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}