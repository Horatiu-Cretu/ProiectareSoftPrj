package com.example.demo.controller;

import com.example.demo.dto.friendrequesdto.FriendRequestDTO;
import com.example.demo.entity.FriendRequest;
import com.example.demo.service.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendRequestController {

    @Autowired
    private FriendRequestService friendRequestService;

    @PostMapping("/send-request")
    public ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDTO requestDTO) {
        friendRequestService.sendFriendRequest(requestDTO);
        return ResponseEntity.ok("Friend request sent successfully");
    }

    @PostMapping("/accept-request")
    public ResponseEntity<String> acceptFriendRequest(@RequestBody FriendRequestDTO requestDTO) {
        friendRequestService.processFriendRequest(requestDTO, 2); // 2 = ACCEPTED
        return ResponseEntity.ok("Friend request accepted");
    }

    @PostMapping("/reject-request")
    public ResponseEntity<String> rejectFriendRequest(@RequestBody FriendRequestDTO requestDTO) {
        friendRequestService.processFriendRequest(requestDTO, 3); // 3 = REJECTED
        return ResponseEntity.ok("Friend request rejected");
    }
}