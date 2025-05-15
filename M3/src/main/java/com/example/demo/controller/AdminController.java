package com.example.demo.controller;

import com.example.demo.dto.AdminActionConfirmationDTO;
import com.example.demo.dto.BlockUserRequestDTO;
import com.example.demo.errorhandler.AdminException;
import com.example.demo.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/m3/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public static final String ORIGINAL_AUTH_HEADER_NAME = "X-Original-Authorization";
    public static final String ADMIN_USER_ID_HEADER = "X-User-ID";


    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private String getOriginalAuthHeaderValue(HttpServletRequest request) throws AdminException {
        String authHeader = request.getHeader(ORIGINAL_AUTH_HEADER_NAME);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Missing or invalid {} header from upstream service (M1).", ORIGINAL_AUTH_HEADER_NAME);
            throw new AdminException("Admin authentication context not properly forwarded.");
        }
        return authHeader;
    }

    private Long getPerformingAdminIdFromRequestAttribute(HttpServletRequest request) throws AdminException {
        Object adminIdAttr = request.getAttribute("userId");
        if (adminIdAttr == null) {
            log.error("Admin User ID not found in request attribute. Ensure X-User-ID header is sent by M1 and processed by M3's filter.");
            throw new AdminException("Admin identity (X-User-ID) not found in request to M3.");
        }
        try {
            return Long.valueOf(adminIdAttr.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid format for Admin User ID in request attribute: {}", adminIdAttr);
            throw new AdminException("Invalid Admin identity format in request to M3.");
        }
    }


    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePostAsAdmin(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long performingAdminId = getPerformingAdminIdFromRequestAttribute(request);
            String originalAuthHeader = getOriginalAuthHeaderValue(request);
            log.info("M3 AdminController: Admin {} deleting post {}.", performingAdminId, postId);
            AdminActionConfirmationDTO confirmation = adminService.deletePostAsAdmin(postId, originalAuthHeader);
            return ResponseEntity.ok(confirmation);
        } catch (AdminException e) {
            log.error("AdminException in M3 AdminController deleting post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in M3 AdminController deleting post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error processing admin request for post deletion.");
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteCommentAsAdmin(@PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long performingAdminId = getPerformingAdminIdFromRequestAttribute(request);
            String originalAuthHeader = getOriginalAuthHeaderValue(request);
            log.info("M3 AdminController: Admin {} deleting comment {}.", performingAdminId, commentId);
            AdminActionConfirmationDTO confirmation = adminService.deleteCommentAsAdmin(commentId, originalAuthHeader);
            return ResponseEntity.ok(confirmation);
        } catch (AdminException e) {
            log.error("AdminException in M3 AdminController deleting comment {}: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in M3 AdminController deleting comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error processing admin request for comment deletion.");
        }
    }

    @PostMapping("/users/{targetUserId}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable Long targetUserId,
            @RequestBody BlockUserRequestDTO blockRequest,
            HttpServletRequest request) {
        try {
            Long performingAdminId = getPerformingAdminIdFromRequestAttribute(request);
            String originalAuthHeader = getOriginalAuthHeaderValue(request);
            log.info("M3 AdminController: Admin {} blocking user {}.", performingAdminId, targetUserId);
            AdminActionConfirmationDTO confirmation = adminService.blockUser(targetUserId, blockRequest, originalAuthHeader);
            return ResponseEntity.ok(confirmation);
        } catch (AdminException e) {
            log.error("AdminException in M3 AdminController blocking user {}: {}", targetUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in M3 AdminController blocking user {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error processing admin request for user blocking.");
        }
    }

    @PostMapping("/users/{targetUserId}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long targetUserId, HttpServletRequest request) {
        try {
            Long performingAdminId = getPerformingAdminIdFromRequestAttribute(request);
            String originalAuthHeader = getOriginalAuthHeaderValue(request);
            log.info("M3 AdminController: Admin {} unblocking user {}.", performingAdminId, targetUserId);
            AdminActionConfirmationDTO confirmation = adminService.unblockUser(targetUserId, originalAuthHeader);
            return ResponseEntity.ok(confirmation);
        } catch (AdminException e) {
            log.error("AdminException in M3 AdminController unblocking user {}: {}", targetUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in M3 AdminController unblocking user {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error processing admin request for user unblocking.");
        }
    }
}
