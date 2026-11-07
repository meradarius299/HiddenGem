package com.hiddengems.repository;

import com.hiddengems.entity.CollectedGem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

import java.util.List;

public interface CollectedGemRepository extends JpaRepository<CollectedGem, Long> {

    boolean existsByUserIdAndGemId(Long userId, Long gemId);

    long countByUserId(Long userId);

    /** Ids of the gems the user has collected. */
    @Query("select c.gem.id from CollectedGem c where c.user.id = :userId")
    List<Long> findGemIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

    /** Everyone with at least one key: most keys first, and on a tie whoever got there first. */
    @Query("""
            select c.user.id as userId, c.user.username as username, count(c) as total
            from CollectedGem c
            where c.user.enabled = true
            group by c.user.id, c.user.username
            order by count(c) desc, max(c.collectedAt) asc
            """)
    List<LeaderboardRow> leaderboard();

    /** The same ranking, limited to the given users (the player and their friends). */
    @Query("""
            select c.user.id as userId, c.user.username as username, count(c) as total
            from CollectedGem c
            where c.user.id in :userIds and c.user.enabled = true
            group by c.user.id, c.user.username
            order by count(c) desc, max(c.collectedAt) asc
            """)
    List<LeaderboardRow> leaderboardAmong(@Param("userIds") Collection<Long> userIds);
}