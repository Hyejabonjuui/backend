package com.hyeja.domain.favorite.repository;

import com.hyeja.domain.favorite.entity.Favorite;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberMemberIdAndPolicyPolicyId(Long memberId, String policyId);

    boolean existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
            Long memberId,
            String policyId
    );

    Optional<Favorite> findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
            Long memberId,
            String policyId
    );

    @Query(
            value = """
                    select favorite
                    from Favorite favorite
                    join fetch favorite.policy policy
                    where favorite.member.memberId = :memberId
                      and favorite.deletedAt is null
                    order by favorite.createdAt desc, favorite.favoriteId desc
                    """,
            countQuery = """
                    select count(favorite)
                    from Favorite favorite
                    where favorite.member.memberId = :memberId
                      and favorite.deletedAt is null
                    """
    )
    Page<Favorite> findAllActiveByMemberId(
            @Param("memberId") Long memberId,
            Pageable pageable
    );
}
