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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** A collected key: records that a user unlocked a gem. One row per user and gem. */
@Entity
@Table(name = "collected_gems",
        uniqueConstraints = @UniqueConstraint(name = "uq_collected_user_gem", columnNames = {"user_id", "gem_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CollectedGem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gem_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // deleting a gem removes its collected keys
    private Gem gem;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();
}