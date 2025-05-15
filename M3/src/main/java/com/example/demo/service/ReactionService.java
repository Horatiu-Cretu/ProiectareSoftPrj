package com.example.demo.service;

import com.example.demo.dto.reaction.ReactionCountUpdateDTO;
import com.example.demo.dto.reaction.ReactionInputDTO;
import com.example.demo.dto.reaction.ReactionViewDTO;
import com.example.demo.entity.Reaction;
import com.example.demo.entity.TargetType;
import com.example.demo.errorhandler.ReactionException;
import com.example.demo.repository.ReactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactionService.class);

    private final ReactionRepository reactionRepository;
    private final RestTemplate restTemplate;
    private final String m2ServiceUrl;

    public ReactionService(ReactionRepository reactionRepository,
                           RestTemplate restTemplate,
                           @Value("${m2.service.url}") String m2ServiceUrl) {
        this.reactionRepository = reactionRepository;
        this.restTemplate = restTemplate;
        this.m2ServiceUrl = m2ServiceUrl;
    }

    @Transactional
    public ReactionViewDTO addOrUpdateReaction(Long userId, ReactionInputDTO inputDTO) throws ReactionException {
        if (userId == null) {
            throw new ReactionException("User ID cannot be null.");
        }

        Optional<Reaction> existingReactionOpt = reactionRepository.findByUserIdAndTargetIdAndTargetType(
                userId, inputDTO.getTargetId(), inputDTO.getTargetType());

        Reaction reaction;
        boolean toDelete = false;

        if (existingReactionOpt.isPresent()) {
            reaction = existingReactionOpt.get();
            if (reaction.getReactionType() == inputDTO.getReactionType()) {
                toDelete = true;
            } else {
                reaction.setReactionType(inputDTO.getReactionType());
                reaction.setUpdatedAt(LocalDateTime.now());
            }
        } else {
            reaction = Reaction.builder()
                    .userId(userId)
                    .targetId(inputDTO.getTargetId())
                    .targetType(inputDTO.getTargetType())
                    .reactionType(inputDTO.getReactionType())
                    .build();
        }

        if (toDelete) {
            reactionRepository.delete(reaction);
            long newCountAfterDelete = reactionRepository.countByTargetIdAndTargetType(inputDTO.getTargetId(), inputDTO.getTargetType());
            updateReactionCountInM2(inputDTO.getTargetId(), inputDTO.getTargetType(), (int) newCountAfterDelete);
            return null;
        } else {
            Reaction savedReaction = reactionRepository.save(reaction);
            long currentCount = reactionRepository.countByTargetIdAndTargetType(inputDTO.getTargetId(), inputDTO.getTargetType());
            updateReactionCountInM2(inputDTO.getTargetId(), inputDTO.getTargetType(), (int) currentCount);
            return convertToViewDTO(savedReaction);
        }
    }

    @Transactional
    public void removeReaction(Long userId, Long targetId, TargetType targetType) throws ReactionException {
        if (userId == null) {
            throw new ReactionException("User ID cannot be null.");
        }
        Optional<Reaction> reactionOpt = reactionRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        if (reactionOpt.isEmpty()) {
            throw new ReactionException("Reaction not found to delete.");
        }
        reactionRepository.delete(reactionOpt.get());
        long currentCount = reactionRepository.countByTargetIdAndTargetType(targetId, targetType);
        updateReactionCountInM2(targetId, targetType, (int) currentCount);
    }

    @Transactional
    public void deleteAllReactionsForTarget(Long targetId, TargetType targetType) {
        log.info("Deleting all reactions for targetId: {} and targetType: {}", targetId, targetType);
        reactionRepository.deleteAllByTargetIdAndTargetType(targetId, targetType);
        log.info("All reactions for targetId: {} and targetType: {} deleted.", targetId, targetType);
    }


    public List<ReactionViewDTO> getReactionsForTarget(Long targetId, TargetType targetType) {
        return reactionRepository.findAllByTargetIdAndTargetType(targetId, targetType)
                .stream()
                .map(this::convertToViewDTO)
                .collect(Collectors.toList());
    }

    public long getReactionCountForTarget(Long targetId, TargetType targetType) {
        return reactionRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    private void updateReactionCountInM2(Long targetId, TargetType targetType, int count) {
        String url;
        if (targetType == TargetType.POST) {
            url = m2ServiceUrl + "/api/m2/posts/" + targetId + "/update-reaction-count";
        } else if (targetType == TargetType.COMMENT) {
            url = m2ServiceUrl + "/api/m2/comments/" + targetId + "/update-reaction-count";
        } else {
            log.error("Unsupported target type for reaction count update: {}", targetType);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ReactionCountUpdateDTO countUpdateDTO = new ReactionCountUpdateDTO(count);
        HttpEntity<ReactionCountUpdateDTO> entity = new HttpEntity<>(countUpdateDTO, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Successfully updated reaction count in M2 for {} {} to {}", targetType, targetId, count);
        } catch (Exception e) {
            log.error("Failed to update reaction count in M2 for {} {}: {}", targetType, targetId, e.getMessage());
        }
    }

    private ReactionViewDTO convertToViewDTO(Reaction reaction) {
        return ReactionViewDTO.builder()
                .id(reaction.getId())
                .userId(reaction.getUserId())
                .targetId(reaction.getTargetId())
                .targetType(reaction.getTargetType())
                .reactionType(reaction.getReactionType())
                .createdAt(reaction.getCreatedAt())
                .updatedAt(reaction.getUpdatedAt())
                .build();
    }
}