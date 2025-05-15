package com.example.demo.repository;

import com.example.demo.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(
            Long senderId,
            Long receiverId,
            Integer status
    );

    @Query("SELECT fr FROM FriendRequest fr " +
            "WHERE (fr.senderId = :userId OR fr.receiverId = :userId) " +
            "AND fr.status = :status")
    List<FriendRequest> findFriendsByUserIdAndStatus(
            Long userId,
            Integer status
    );

    Optional<FriendRequest> findById(Long id);

    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);

    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, Integer status);

    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, Integer status);

    @Query("SELECT fr FROM FriendRequest fr " +
            "WHERE fr.senderId = :userId OR fr.receiverId = :userId")
    List<FriendRequest> findAllRequestsForUser(Long userId);

    List<FriendRequest> findByReceiverId(Long receiverId);

    List<FriendRequest> findBySenderId(Long senderId);

    @Query("DELETE FROM FriendRequest fr " +
            "WHERE (fr.senderId = :user1Id AND fr.receiverId = :user2Id) " +
            "OR (fr.senderId = :user2Id AND fr.receiverId = :user1Id)")
    void deleteRequestsBetweenUsers(Long user1Id, Long user2Id);

    long countByReceiverIdAndStatus(Long receiverId, Integer status);
}