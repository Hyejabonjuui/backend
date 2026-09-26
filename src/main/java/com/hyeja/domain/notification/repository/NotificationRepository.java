package com.hyeja.domain.notification.repository;

import com.hyeja.domain.notification.entity.Notification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository 
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 회원 탈퇴 시 그 회원의 알림을 모두 지웁니다 (hard delete).
    void deleteByMemberMemberId(Long memberId);

    Optional<Notification> findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(
            Long notificationId,
            Long memberId
    );

    @Query(
            value = """
                    select notification
                    from Notification notification
                    join fetch notification.member member
                    join fetch notification.policy policy
                    where member.memberId = :memberId
                      and notification.deletedAt is null
                    order by notification.createdAt desc, notification.notificationId desc
                    """,
            countQuery = """
                    select count(notification)
                    from Notification notification
                    where notification.member.memberId = :memberId
                      and notification.deletedAt is null
                    """
    )
    Page<Notification> findAllByMemberId(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            select notification
            from Notification notification
            join fetch notification.member member
            join fetch notification.policy policy
            where notification.deadlineDate = :deadlineDate
            """)
    List<Notification> findAllByDeadlineDateWithMemberAndPolicy(
            @Param("deadlineDate") LocalDate deadlineDate
    );

    @Query("""
            select notification
            from Notification notification
            join fetch notification.member member
            join fetch notification.policy policy
            where member.memberId = :memberId
              and notification.deadlineDate = :deadlineDate
            """)
    List<Notification> findAllByMemberIdAndDeadlineDateWithPolicy(
            @Param("memberId") Long memberId,
            @Param("deadlineDate") LocalDate deadlineDate
    );
}
