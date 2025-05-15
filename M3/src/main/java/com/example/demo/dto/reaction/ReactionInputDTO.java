package com.example.demo.dto.reaction;



import com.example.demo.entity.ReactionType;
import com.example.demo.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionInputDTO {

    @NotNull
    private Long targetId;

    @NotNull
    private TargetType targetType;

    @NotNull
    private ReactionType reactionType;
}