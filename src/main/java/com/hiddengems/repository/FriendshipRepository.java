package com.hiddengems.repository;

import com.hiddengems.entity.Friendship;
import com.hiddengems.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Relatia dintre doi utilizatori, indiferent de directie. */
    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b)
               or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    /** Relatiile utilizatorului (in oricare directie) cu statusul dat. */
    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where f.status = :status
              and (f.requester.id = :userId or f.addressee.id = :userId)
            """)
    List<Friendship> findByUserAndStatus(@Param("userId") Long userId,
                                         @Param("status") FriendshipStatus status);

    /** Cererile primite de utilizator, cu statusul dat. */
    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where f.addressee.id = :userId and f.status = :status
            order by f.createdAt desc
            """)
    List<Friendship> findIncoming(@Param("userId") Long userId,
                                  @Param("status") FriendshipStatus status);

    /** Id-urile prietenilor (relatii ACCEPTED) ale utilizatorului. */
    @Query("""
            select case when f.requester.id = :userId then f.addressee.id else f.requester.id end
            from Friendship f
            where f.status = :status
              and (f.requester.id = :userId or f.addressee.id = :userId)
            """)
    List<Long> findFriendIds(@Param("userId") Long userId,
                             @Param("status") FriendshipStatus status);
}
