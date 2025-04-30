package com.example.demo.builder.friendrequestbuilder;

import com.example.demo.entity.FriendRequest;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

public class FriendRequestViewBuilder {

    @Data
    @Builder
    public static class FriendRequestViewDTO {
        private Long idSender;
        private Long idReceiver;
        private String status;
        private String createdAt;
    }

    public static FriendRequestViewDTO generateDTOFromEntity(FriendRequest friendRequest) {
        return FriendRequestViewDTO.builder()
                .idSender(friendRequest.getSenderId())
                .idReceiver(friendRequest.getReceiverId())
                .status(convertStatus(friendRequest.getStatus()))
                .createdAt(friendRequest.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss")))
                .build();
    }

    private static String convertStatus(Integer status) {
        return switch (status) {
            case 1 -> "PENDING";
            case 2 -> "ACCEPTED";
            case 3 -> "REJECTED";
            default -> "UNKNOWN";
        };
    }
}