package com.example.demo.service;

        import com.example.demo.dto.friendrequesdto.FriendRequestDTO;
        import com.example.demo.entity.FriendRequest;
        import com.example.demo.errorhandler.UserException;
        import com.example.demo.repository.FriendRequestRepository;
        import com.example.demo.repository.UserRepository;
        import lombok.RequiredArgsConstructor;
        import org.springframework.stereotype.Service;

        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.stream.Collectors;

        @Service
        @RequiredArgsConstructor
        public class FriendRequestService {
            private final FriendRequestRepository friendRequestRepository;
            private final UserRepository userRepository;

            public FriendRequestDTO sendFriendRequest(Long senderId, Long receiverId) throws UserException {
                // Validate users exist
                if (!userRepository.existsById(senderId)) {
                    throw new UserException("Sender not found");
                }
                if (!userRepository.existsById(receiverId)) {
                    throw new UserException("Receiver not found");
                }

                // Check if request already exists
                if (friendRequestRepository.existsBySenderIdAndReceiverId(senderId, receiverId)) {
                    throw new UserException("Friend request already exists");
                }

                // Check if they're already friends
                if (areFriends(senderId, receiverId)) {
                    throw new UserException("Users are already friends");
                }

                FriendRequest request = new FriendRequest(senderId, receiverId);
                return convertToDTO(friendRequestRepository.save(request));
            }

            public FriendRequestDTO acceptFriendRequest(Long requestId, Long receiverId) throws UserException {
                FriendRequest request = friendRequestRepository.findById(requestId)
                    .orElseThrow(() -> new UserException("Friend request not found"));

                // Verify the receiver is the one accepting the request
                if (!request.getReceiverId().equals(receiverId)) {
                    throw new UserException("Unauthorized to accept this request");
                }

                if (request.getStatus() != 1) { // 1 = PENDING
                    throw new UserException("Request is not pending");
                }

                request.setStatus(2); // 2 = ACCEPTED
                return convertToDTO(friendRequestRepository.save(request));
            }

            public FriendRequestDTO rejectFriendRequest(Long requestId, Long receiverId) throws UserException {
                FriendRequest request = friendRequestRepository.findById(requestId)
                    .orElseThrow(() -> new UserException("Friend request not found"));

                // Verify the receiver is the one rejecting the request
                if (!request.getReceiverId().equals(receiverId)) {
                    throw new UserException("Unauthorized to reject this request");
                }

                if (request.getStatus() != 1) { // 1 = PENDING
                    throw new UserException("Request is not pending");
                }

                request.setStatus(3); // 3 = REJECTED
                return convertToDTO(friendRequestRepository.save(request));
            }

            public List<FriendRequestDTO> getPendingRequests(Long userId) {
                return friendRequestRepository.findByReceiverIdAndStatus(userId, 1)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            }

            private boolean areFriends(Long user1Id, Long user2Id) {
                return friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(user1Id, user2Id, 2) ||
                       friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(user2Id, user1Id, 2);
            }

            private FriendRequestDTO convertToDTO(FriendRequest request) {
                return FriendRequestDTO.builder()
                        .idSender(request.getSenderId())
                        .idReceiver(request.getReceiverId())
                        .build();
            }
        }
