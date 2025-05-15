package com.example.demo.controller;


import com.example.demo.dto.reaction.ReactionInputDTO;
import com.example.demo.dto.reaction.ReactionViewDTO;
import com.example.demo.entity.TargetType;
import com.example.demo.errorhandler.ReactionException;
import com.example.demo.service.ReactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    private Long getCurrentUserId(HttpServletRequest request) throws ReactionException {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            throw new ReactionException("User ID not found in request. Ensure M1 sets X-User-ID header.");
        }
        try {
            return Long.valueOf(userIdAttr.toString());
        } catch (NumberFormatException e) {
            throw new ReactionException("Invalid User ID format in request attribute.");
        }
    }

    @PostMapping
    public ResponseEntity<ReactionViewDTO> addOrUpdateReaction( @RequestBody ReactionInputDTO inputDTO,
                                                               HttpServletRequest request) throws ReactionException {
        Long userId = getCurrentUserId(request);
        ReactionViewDTO reactionViewDTO = reactionService.addOrUpdateReaction(userId, inputDTO);
        if (reactionViewDTO == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reactionViewDTO);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeReaction(@RequestParam Long targetId,
                                               @RequestParam TargetType targetType,
                                               HttpServletRequest request) throws ReactionException {
        Long userId = getCurrentUserId(request);
        reactionService.removeReaction(userId, targetId, targetType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<List<ReactionViewDTO>> getReactionsForTarget(@PathVariable TargetType targetType,
                                                                       @PathVariable Long targetId) {
        List<ReactionViewDTO> reactions = reactionService.getReactionsForTarget(targetId, targetType);
        return ResponseEntity.ok(reactions);
    }

    @GetMapping("/target/{targetType}/{targetId}/count")
    public ResponseEntity<Long> getReactionCountForTarget(@PathVariable TargetType targetType,
                                                          @PathVariable Long targetId) {
        long count = reactionService.getReactionCountForTarget(targetId, targetType);
        return ResponseEntity.ok(count);
    }
}