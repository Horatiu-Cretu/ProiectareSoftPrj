package com.example.demo.builder.friendrequestbuilder;

import com.example.demo.dto.friendrequesdto.FriendRequestDTO;
import com.example.demo.entity.FriendRequest;

public class FriendRequestBuilder {

    public static FriendRequest generateEntityFromDTO(FriendRequestDTO friendRequestDTO) {
        return new FriendRequest(
            friendRequestDTO.getIdSender(),
            friendRequestDTO.getIdReceiver()
        );
    }

    public static FriendRequestDTO generateDTOFromEntity(FriendRequest friendRequest) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setIdSender(friendRequest.getSenderId());
        dto.setIdReceiver(friendRequest.getReceiverId());
        return dto;
    }
}