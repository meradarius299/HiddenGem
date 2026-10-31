package com.hiddengems.repository;

import com.hiddengems.entity.Gem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GemRepository extends JpaRepository<Gem, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Gem> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "owner")
    Optional<Gem> findWithOwnerById(Long id);

    /** Locuri aflate la cel mult radiusKm de punctul (lat, lng) - formula Haversine. */
    @Query(value = """
            SELECT g.* FROM gems g
            WHERE 6371 * acos(LEAST(1,
                    cos(radians(:lat)) * cos(radians(g.latitude))
                  * cos(radians(g.longitude) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(g.latitude)))) <= :radiusKm
            ORDER BY g.created_at DESC
            """, nativeQuery = true)
    List<Gem> findNearby(@Param("lat") double lat,
                         @Param("lng") double lng,
                         @Param("radiusKm") double radiusKm);
}
