package com.example.demo.service;

import com.example.demo.dto.AdminActionConfirmationDTO;
import com.example.demo.dto.BlockUserRequestDTO;
import com.example.demo.entity.TargetType;
import com.example.demo.errorhandler.AdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final RestTemplate restTemplate;
    private final ReactionService reactionService;

    private final String m1ServiceUrl;
    private final String m2ServiceUrl;

    public static final String ORIGINAL_AUTH_HEADER_NAME = "X-Original-Authorization";


    public AdminService(RestTemplate restTemplate,
                        ReactionService reactionService,
                        @Value("${m1.service.url}") String m1ServiceUrl,
                        @Value("${m2.service.url}") String m2ServiceUrl) {
        this.restTemplate = restTemplate;
        this.reactionService = reactionService;
        this.m1ServiceUrl = m1ServiceUrl;
        this.m2ServiceUrl = m2ServiceUrl;
    }

    private HttpHeaders createHeadersWithForwardedAuth(String originalAuthHeaderValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (originalAuthHeaderValue != null && originalAuthHeaderValue.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, originalAuthHeaderValue);
        } else {
            log.warn("Original Authorization header was not provided or not a Bearer token. Downstream calls might fail if they require authentication.");
        }
        return headers;
    }

    @Transactional
    public AdminActionConfirmationDTO deletePostAsAdmin(Long postId, String originalAuthHeaderValue) throws AdminException {
        String url = m2ServiceUrl + "/api/m2/posts/admin/posts/" + postId;
        log.info("Admin (via M3) requesting deletion of post {} from M2 URL: {}", postId, url);

        try {
            HttpEntity<?> requestEntity = new HttpEntity<>(createHeadersWithForwardedAuth(originalAuthHeaderValue));
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
            log.info("Post {} successfully deleted in M2 by admin.", postId);

            reactionService.deleteAllReactionsForTarget(postId, TargetType.POST);
            log.info("Reactions for post {} cleaned up in M3.", postId);

            return new AdminActionConfirmationDTO("Post and associated reactions deleted successfully by admin.", postId, "DELETE_POST");
        } catch (HttpClientErrorException e) {
            log.error("Client error from M2 while admin deleting post {}: {} - {}", postId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AdminException("Failed to delete post in M2: " + e.getStatusCode() + " (" + e.getResponseBodyAsString() + ")", e);
        } catch (RestClientException e) {
            log.error("Error connecting to M2 while admin deleting post {}: {}", postId, e.getMessage(), e);
            throw new AdminException("Could not connect to post service to delete post.", e);
        } catch (Exception e) {
            log.error("Error during admin deletion of post {} or its reactions: {}", postId, e.getMessage(), e);
            throw new AdminException("Failed to complete post deletion process: " + e.getMessage(), e);
        }
    }

    @Transactional
    public AdminActionConfirmationDTO deleteCommentAsAdmin(Long commentId, String originalAuthHeaderValue) throws AdminException {
        String url = m2ServiceUrl + "/api/m2/comments/admin/comments/" + commentId;
        log.info("Admin (via M3) requesting deletion of comment {} from M2 URL: {}", commentId, url);

        try {
            HttpEntity<?> requestEntity = new HttpEntity<>(createHeadersWithForwardedAuth(originalAuthHeaderValue));
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
            log.info("Comment {} successfully deleted in M2 by admin.", commentId);

            reactionService.deleteAllReactionsForTarget(commentId, TargetType.COMMENT);
            log.info("Reactions for comment {} cleaned up in M3.", commentId);

            return new AdminActionConfirmationDTO("Comment and associated reactions deleted successfully by admin.", commentId, "DELETE_COMMENT");
        } catch (HttpClientErrorException e) {
            log.error("Client error from M2 while admin deleting comment {}: {} - {}", commentId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AdminException("Failed to delete comment in M2: " + e.getStatusCode() + " (" + e.getResponseBodyAsString() + ")", e);
        } catch (RestClientException e) {
            log.error("Error connecting to M2 while admin deleting comment {}: {}", commentId, e.getMessage(), e);
            throw new AdminException("Could not connect to comment service to delete comment.", e);
        } catch (Exception e) {
            log.error("Error during admin deletion of comment {} or its reactions: {}", commentId, e.getMessage(), e);
            throw new AdminException("Failed to complete comment deletion process: " + e.getMessage(), e);
        }
    }

    public AdminActionConfirmationDTO blockUser(Long targetUserId, BlockUserRequestDTO blockRequest, String originalAuthHeaderValue) throws AdminException {
        String url = m1ServiceUrl + "/api/admin/users/" + targetUserId + "/block";
        log.info("Admin (via M3) requesting to block user {} via M1 URL: {}", targetUserId, url);

        try {
            HttpEntity<BlockUserRequestDTO> requestEntity = new HttpEntity<>(blockRequest, createHeadersWithForwardedAuth(originalAuthHeaderValue));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("User {} successfully blocked in M1 by admin. Response from M1: {}", targetUserId, response.getBody());
            return new AdminActionConfirmationDTO(response.getBody(), targetUserId, "BLOCK_USER");
        } catch (HttpClientErrorException e) {
            log.error("Client error from M1 while admin blocking user {}: {} - {}", targetUserId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AdminException("Failed to block user in M1: " + e.getStatusCode() + " (" + e.getResponseBodyAsString() + ")", e);
        } catch (RestClientException e) {
            log.error("Error connecting to M1 while admin blocking user {}: {}", targetUserId, e.getMessage(), e);
            throw new AdminException("Could not connect to user service to block user.", e);
        }
    }

    public AdminActionConfirmationDTO unblockUser(Long targetUserId, String originalAuthHeaderValue) throws AdminException {
        String url = m1ServiceUrl + "/api/admin/users/" + targetUserId + "/unblock";
        log.info("Admin (via M3) requesting to unblock user {} via M1 URL: {}", targetUserId, url);

        try {
            HttpEntity<?> requestEntity = new HttpEntity<>(createHeadersWithForwardedAuth(originalAuthHeaderValue));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("User {} successfully unblocked in M1 by admin. Response from M1: {}", targetUserId, response.getBody());
            return new AdminActionConfirmationDTO(response.getBody(), targetUserId, "UNBLOCK_USER");
        } catch (HttpClientErrorException e) {
            log.error("Client error from M1 while admin unblocking user {}: {} - {}", targetUserId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AdminException("Failed to unblock user in M1: " + e.getStatusCode() + " (" + e.getResponseBodyAsString() + ")", e);
        } catch (RestClientException e) {
            log.error("Error connecting to M1 while admin unblocking user {}: {}", targetUserId, e.getMessage(), e);
            throw new AdminException("Could not connect to user service to unblock user.", e);
        }
    }
}