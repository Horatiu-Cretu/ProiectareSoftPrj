package com.example.demo.repository;

import com.example.demo.entity.Reaction;
import com.example.demo.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    List<Reaction> findAllByTargetIdAndTargetType(Long targetId, TargetType targetType);

    long countByTargetIdAndTargetType(Long targetId, TargetType targetType);

    void deleteByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.targetId = :targetId AND r.targetType = :targetType")
    void deleteAllByTargetIdAndTargetType(@Param("targetId") Long targetId, @Param("targetType") TargetType targetType);
}