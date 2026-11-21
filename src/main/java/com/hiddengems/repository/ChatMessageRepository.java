package com.hiddengems.repository;

import com.hiddengems.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Newest first. afterId = 0 gives the latest messages, otherwise only newer ones. */
    @Query("""
            select m from ChatMessage m
            where m.id > :afterId
              and ((m.sender.id = :a and m.recipient.id = :b)
                or (m.sender.id = :b and m.recipient.id = :a))
            order by m.id desc
            """)
    List<ChatMessage> findConversation(@Param("a") Long a, @Param("b") Long b,
                                       @Param("afterId") long afterId, Pageable pageable);

    @Query("""
            select m.sender.id as senderId, count(m) as total
            from ChatMessage m
            where m.recipient.id = :userId and m.seen = false
            group by m.sender.id
            """)
    List<UnreadChatRow> unreadBySender(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update ChatMessage m set m.seen = true "
            + "where m.recipient.id = :me and m.sender.id = :friend and m.seen = false")
    int markConversationRead(@Param("me") Long me, @Param("friend") Long friend);
}