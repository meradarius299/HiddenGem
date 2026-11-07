package com.hiddengems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A badge a user has earned. One row per user and badge. */
@Entity
@Table(name = "user_badges",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_badge", columnNames = {"user_id", "badge_code"}))
@Getter
@Setter
@NoArgsConstructor
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The name of a {@link Badge}. Stored as plain text so adding a badge later needs no schema change. */
    @Column(name = "badge_code", nullable = false, length = 30)
    private String badgeCode;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt = LocalDateTime.now();
}