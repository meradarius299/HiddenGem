package com.hiddengems.entity;

/** Trebuie sa corespunda cu CHECK-ul coloanei notifications.type din schema.sql. */
public enum NotificationType {
    NEW_GEM,
    TASK_COMPLETED,
    BADGE_EARNED,
    REVIEW_ADDED,
    FRIEND_REQUEST,
    BUMP,
    INACTIVITY_REMINDER
}
