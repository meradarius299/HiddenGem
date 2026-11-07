package com.hiddengems.repository;

import com.hiddengems.entity.CollectedGem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectedGemRepository extends JpaRepository<CollectedGem, Long> {

    boolean existsByUserIdAndGemId(Long userId, Long gemId);

    long countByUserId(Long userId);

    /** Ids of the gems the user has collected. */
    @Query("select c.gem.id from CollectedGem c where c.user.id = :userId")
    List<Long> findGemIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}