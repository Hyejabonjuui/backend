package com.hyeja.domain.favorite.repository;

import com.hyeja.domain.favorite.entity.Favorite;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberMemberIdAndPolicyPolicyId(Long memberId, String policyId);

    Optional<Favorite> findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
            Long memberId,
            String policyId
    );

    @Query("""
            select favorite.policy.policyId
            from Favorite favorite
            where favorite.member.memberId = :memberId
              and favorite.policy.policyId in :policyIds
              and favorite.deletedAt is null
            """)
    Set<String> findActivePolicyIds(
            @Param("memberId") Long memberId,
            @Param("policyIds") Collection<String> policyIds
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

    @Query("""
            select favorite
            from Favorite favorite
            join fetch favorite.member member
            join fetch favorite.policy policy
            where favorite.deletedAt is null
              and member.deletedAt is null
              and policy.deletedAt is null
              and policy.activeYn = true
              and policy.applyEndDate = :deadlineDate
            order by favorite.favoriteId asc
            """)
    List<Favorite> findNotificationTargetsByDeadlineDate(
            @Param("deadlineDate") LocalDate deadlineDate
    );

    @Query("""
            select favorite
            from Favorite favorite
            join fetch favorite.member member
            join fetch favorite.policy policy
            where member.memberId = :memberId
              and favorite.deletedAt is null
              and member.deletedAt is null
              and policy.deletedAt is null
              and policy.activeYn = true
              and policy.applyEndDate = :deadlineDate
            order by favorite.favoriteId asc
            """)
    List<Favorite> findNotificationTargetsByMemberIdAndDeadlineDate(
            @Param("memberId") Long memberId,
            @Param("deadlineDate") LocalDate deadlineDate
    );

    @Query(
            value = """
                    select favorite
                    from Favorite favorite
                    join fetch favorite.policy policy
                    where favorite.member.memberId = :memberId
                      and favorite.deletedAt is null
                      and (
                          lower(policy.policyName) like lower(concat('%', :keyword, '%')) escape '!'
                          or lower(policy.supportContent) like lower(concat('%', :keyword, '%')) escape '!'
                      )
                    order by favorite.createdAt desc, favorite.favoriteId desc
                    """,
            countQuery = """
                    select count(favorite)
                    from Favorite favorite
                    join favorite.policy policy
                    where favorite.member.memberId = :memberId
                      and favorite.deletedAt is null
                      and (
                          lower(policy.policyName) like lower(concat('%', :keyword, '%')) escape '!'
                          or lower(policy.supportContent) like lower(concat('%', :keyword, '%')) escape '!'
                      )
                    """
    )
    Page<Favorite> searchAllActiveByMemberIdAndKeyword(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
