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
            Integer status // Use integer for status (1 = PENDING, 2 = ACCEPTED, 3 = REJECTED)
    );

    Optional<FriendRequest> findById(Long id);
}