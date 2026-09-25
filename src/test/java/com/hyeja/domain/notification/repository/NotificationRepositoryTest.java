package com.hyeja.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsOnlyActiveNotificationsForRequestedMember() {
        Member requestedMember = persistMember("member@example.com", "회원");
        Member otherMember = persistMember("other@example.com", "다른 회원");
        Policy firstPolicy = persistPolicy("policy-1", LocalDate.of(2026, 10, 1));
        Policy secondPolicy = persistPolicy("policy-2", LocalDate.of(2026, 11, 1));

        Notification first = Notification.builder()
                .member(requestedMember).policy(firstPolicy).build();
        Notification deleted = Notification.builder()
                .member(requestedMember).policy(secondPolicy).build();
        Notification other = Notification.builder()
                .member(otherMember).policy(firstPolicy).build();
        entityManager.persist(first);
        entityManager.persist(deleted);
        entityManager.persist(other);
        deleted.softDelete();
        entityManager.flush();
        entityManager.clear();

        var result = notificationRepository.findAllByMemberId(
                requestedMember.getMemberId(),
                PageRequest.of(0, 8)
        );

        assertThat(result.getContent()).singleElement().satisfies(notification -> {
            assertThat(notification.getNotificationId()).isEqualTo(first.getNotificationId());
            assertThat(notification.getPolicy().getPolicyId()).isEqualTo("policy-1");
            assertThat(notification.getPolicy().getApplyEndDate())
                    .isEqualTo(LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void findsOnlyActiveNotificationOwnedByRequestedMember() {
        Member requestedMember = persistMember("member@example.com", "회원");
        Member otherMember = persistMember("other@example.com", "다른 회원");
        Policy policy = persistPolicy("policy-1", LocalDate.of(2026, 10, 1));
        Notification active = Notification.builder()
                .member(requestedMember).policy(policy).build();
        Notification deleted = Notification.builder()
                .member(requestedMember).policy(policy).build();
        Notification other = Notification.builder()
                .member(otherMember).policy(policy).build();
        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.persist(other);
        deleted.softDelete();
        entityManager.flush();
        entityManager.clear();

        assertThat(notificationRepository
                .findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(
                        active.getNotificationId(), requestedMember.getMemberId()))
                .isPresent();
        assertThat(notificationRepository
                .findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(
                        deleted.getNotificationId(), requestedMember.getMemberId()))
                .isEmpty();
        assertThat(notificationRepository
                .findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(
                        other.getNotificationId(), requestedMember.getMemberId()))
                .isEmpty();
    }

    private Member persistMember(String email, String nickname) {
        Member member = Member.builder()
                .email(email).password("encoded-password").nickname(nickname).build();
        entityManager.persist(member);
        return member;
    }

    private Policy persistPolicy(String policyId, LocalDate applyEndDate) {
        Policy policy = Policy.builder()
                .policyId(policyId)
                .policyName("테스트 정책")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(false)
                .applyPeriodCode("PERIOD")
                .applyEndDate(applyEndDate)
                .build();
        entityManager.persist(policy);
        return policy;
    }
}
