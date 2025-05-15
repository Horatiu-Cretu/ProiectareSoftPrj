package com.example.demo.controller.forwardingControllers;
import com.example.demo.dto.admindto.BlockUserRequestDTO;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.JWTService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/admin")
public class AdminForwardingController {

    private static final Logger log = LoggerFactory.getLogger(AdminForwardingController.class);
    private final UserService userService;
    private final JWTService jwtService;
    private final RestTemplate restTemplate;
    private final String m3AdminBaseUrl;

    public AdminForwardingController(UserService userService,
                                     JWTService jwtService,
                                     RestTemplate restTemplate,
                                     @Value("${m3.service.url}") String m3BaseUrl) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
        this.m3AdminBaseUrl = m3BaseUrl + "/api/m3/admin";
    }

    private Long getCurrentAdminId(HttpServletRequest request) throws UserException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UserException("Authorization header is missing or invalid.");
        }
        String token = authHeader.substring(7);
        Long adminId = jwtService.extractUserId(token);
        if (adminId == null) {
            throw new UserException("Could not extract admin ID from token.");
        }
        return adminId;
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> adminDeletePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            String originalAuthHeader = request.getHeader("Authorization");
            log.info("M1: Admin {} attempting to delete post {}", adminId, postId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", String.valueOf(adminId));
            if (originalAuthHeader != null) {
                headers.set("X-Original-Authorization", originalAuthHeader);
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String targetUrl = m3AdminBaseUrl + "/posts/" + postId;
            log.debug("M1: Forwarding admin delete post request for postId {} to M3 URL: {}", postId, targetUrl);

            ResponseEntity<String> m3Response = restTemplate.exchange(targetUrl, HttpMethod.DELETE, entity, String.class);
            return ResponseEntity.status(m3Response.getStatusCode()).body(m3Response.getBody());

        } catch (UserException e) {
            log.error("M1: UserException during admin delete post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("M1: HttpStatusCodeException from M3 during admin delete post {}: {} - {}", postId, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("M1: Unexpected error during admin delete post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred in M1 while forwarding admin post deletion.");
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> adminDeleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            String originalAuthHeader = request.getHeader("Authorization");
            log.info("M1: Admin {} attempting to delete comment {}", adminId, commentId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", String.valueOf(adminId));
            if (originalAuthHeader != null) {
                headers.set("X-Original-Authorization", originalAuthHeader);
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String targetUrl = m3AdminBaseUrl + "/comments/" + commentId;
            log.debug("M1: Forwarding admin delete comment request for commentId {} to M3 URL: {}", commentId, targetUrl);

            ResponseEntity<String> m3Response = restTemplate.exchange(targetUrl, HttpMethod.DELETE, entity, String.class);
            return ResponseEntity.status(m3Response.getStatusCode()).body(m3Response.getBody());

        } catch (UserException e) {
            log.error("M1: UserException during admin delete comment {}: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("M1: HttpStatusCodeException from M3 during admin delete comment {}: {} - {}", commentId, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("M1: Unexpected error during admin delete comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred in M1 while forwarding admin comment deletion.");
        }
    }

    @PostMapping("/users/{targetUserId}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable Long targetUserId,
            @RequestBody BlockUserRequestDTO blockRequest,
            HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            String originalAuthHeader = request.getHeader("Authorization");
            log.info("M1: Admin {} attempting to block user {}", adminId, targetUserId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-ID", String.valueOf(adminId));
            if (originalAuthHeader != null) {
                headers.set("X-Original-Authorization", originalAuthHeader);
            }

            HttpEntity<BlockUserRequestDTO> entity = new HttpEntity<>(blockRequest, headers);
            String targetUrl = m3AdminBaseUrl + "/users/" + targetUserId + "/block";
            log.debug("M1: Forwarding block user request for {} to M3 URL: {}", targetUserId, targetUrl);

            ResponseEntity<String> m3Response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.status(m3Response.getStatusCode()).body(m3Response.getBody());

        } catch (UserException e) {
            log.error("M1: Error blocking user {}: {}", targetUserId, e.getMessage());
            HttpStatus status = e.getMessage().toLowerCase().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            if (e.getMessage().toLowerCase().contains("not authorized")) {
                status = HttpStatus.FORBIDDEN;
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("M1: HttpStatusCodeException from M3 during block user {}: {} - {}", targetUserId, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("M1: Unexpected error blocking user {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PostMapping("/users/{targetUserId}/unblock")
    public ResponseEntity<?> unblockUser(
            @PathVariable Long targetUserId,
            HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            String originalAuthHeader = request.getHeader("Authorization");
            log.info("M1: Admin {} attempting to unblock user {}", adminId, targetUserId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-ID", String.valueOf(adminId));
            if (originalAuthHeader != null) {
                headers.set("X-Original-Authorization", originalAuthHeader);
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String targetUrl = m3AdminBaseUrl + "/users/" + targetUserId + "/unblock";
            log.debug("M1: Forwarding unblock user request for {} to M3 URL: {}", targetUserId, targetUrl);

            ResponseEntity<String> m3Response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.status(m3Response.getStatusCode()).body(m3Response.getBody());

        } catch (UserException e) {
            log.error("M1: Error unblocking user {}: {}", targetUserId, e.getMessage());
            HttpStatus status = e.getMessage().toLowerCase().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            if (e.getMessage().toLowerCase().contains("not authorized")) {
                status = HttpStatus.FORBIDDEN;
            }
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("M1: HttpStatusCodeException from M3 during unblock user {}: {} - {}", targetUserId, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("M1: Unexpected error unblocking user {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}