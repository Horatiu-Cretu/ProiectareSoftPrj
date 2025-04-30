package com.example.demo.controller;

import com.example.demo.dto.friendrequesdto.FriendRequestDTO;
import com.example.demo.entity.FriendRequest;
import com.example.demo.service.FriendRequestService;
import com.example.demo.errorhandler.UserException;
import com.example.demo.service.JWTService;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendRequestController {
    private final FriendRequestService friendRequestService;
    private final JWTService jwtService;
    private final UserRepository userRepository;

    public FriendRequestController(FriendRequestService friendRequestService, 
                                 JWTService jwtService,
                                 UserRepository userRepository) {

        this.friendRequestService = friendRequestService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send/{receiverId}")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(
            @PathVariable Long receiverId,
            @RequestHeader("Authorization") String authHeader) throws UserException {
        Long senderId = getCurrentUserIdFromToken(authHeader);
        return ResponseEntity.ok(friendRequestService.sendFriendRequest(senderId, receiverId));
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<FriendRequestDTO> acceptFriendRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) throws UserException {
        Long receiverId = getCurrentUserIdFromToken(authHeader);
        return ResponseEntity.ok(friendRequestService.acceptFriendRequest(requestId, receiverId));
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<FriendRequestDTO> rejectFriendRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) throws UserException {
        Long receiverId = getCurrentUserIdFromToken(authHeader);
        return ResponseEntity.ok(friendRequestService.rejectFriendRequest(requestId, receiverId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendRequestDTO>> getPendingRequests(
            @RequestHeader("Authorization") String authHeader) throws UserException {
        Long userId = getCurrentUserIdFromToken(authHeader);
        return ResponseEntity.ok(friendRequestService.getPendingRequests(userId));
    }

    private Long getCurrentUserIdFromToken(String authHeader) throws UserException {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            return getUserIdFromEmail(email);
        }
        throw new UserException("Invalid or missing authorization token");
    }

    private Long getUserIdFromEmail(String email) throws UserException {
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserException("User not found for email: " + email))
                .getId();
    }
}
