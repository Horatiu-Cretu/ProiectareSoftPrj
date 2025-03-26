package com.example.demo.service;

import com.example.demo.dto.friendrequesdto.FriendRequestDTO;
import com.example.demo.entity.FriendRequest;
import com.example.demo.repository.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendRequestService {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Transactional
    public void sendFriendRequest(FriendRequestDTO requestDTO) {
        // Check if a pending request already exists
        if (friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                requestDTO.getIdSender(),
                requestDTO.getIdReceiver(),
                1).isPresent()) { // 1 = PENDING
            throw new RuntimeException("Friend request already sent");
        }

        // Create and save new friend request
        FriendRequest friendRequest = new FriendRequest(
                requestDTO.getIdSender(),
                requestDTO.getIdReceiver()
        );
        friendRequestRepository.save(friendRequest);
    }

    @Transactional
    public void processFriendRequest(FriendRequestDTO requestDTO, Integer status) {

        // Find the pending request
        FriendRequest friendRequest = friendRequestRepository
                .findBySenderIdAndReceiverIdAndStatus(
                        requestDTO.getIdSender(),
                        requestDTO.getIdReceiver(),
                        1) // 1 = PENDING
                .orElseThrow(() -> new RuntimeException("No pending friend request found"));

        // Update request status
        friendRequest.setStatus(status); // 2 = ACCEPTED, 3 = REJECTED
        friendRequestRepository.save(friendRequest);
    }
}