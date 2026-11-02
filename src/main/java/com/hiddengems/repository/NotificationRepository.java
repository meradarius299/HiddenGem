package com.hiddengems.repository;

import com.hiddengems.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"actor", "gem"})
    List<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndSeenFalse(Long recipientId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true where n.id = :id and n.recipient.id = :userId")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true where n.recipient.id = :userId and n.seen = false")
    int markAllRead(@Param("userId") Long userId);
}
