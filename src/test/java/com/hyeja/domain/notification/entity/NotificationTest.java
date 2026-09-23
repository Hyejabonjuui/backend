package com.hyeja.domain.notification.entity;

import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NotificationTest {

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Policy policy;

    @BeforeEach
    void setUp() {
        member = Member.builder().email("member@example.com")
                .password("encoded-password").nickname("회원").build();
        policy = Policy.builder().policyId("policy-1").policyName("테스트 정책")
                .category(PolicyCategory.MONTHLY_RENT).ageLimitYn(false).applyPeriodCode("TEST").build();
        entityManager.persist(member);
        entityManager.persist(policy);
        entityManager.flush();
    }

    @Test
    void persistsUnreadNotificationAndLoadsAssociationsLazily() {
        Notification notification = Notification.builder().member(member).policy(policy).build();
        entityManager.persist(notification);
        entityManager.flush();
        entityManager.clear();

        Notification stored = entityManager.find(Notification.class, notification.getNotificationId());
        assertThat(stored.getNotificationId()).isNotNull();
        assertThat(stored.getReadYn()).isFalse();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.getDeletedAt()).isNull();
        var persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(stored, "member")).isFalse();
        assertThat(persistenceUnitUtil.isLoaded(stored, "policy")).isFalse();
        assertThat(stored.getMember().getEmail()).isEqualTo("member@example.com");
        assertThat(stored.getPolicy().getPolicyName()).isEqualTo("테스트 정책");
    }

    @Test
    void marksOnlySelectedNotificationAsReadAndRepeatedCallsKeepItRead() {
        Notification first = Notification.builder().member(member).policy(policy).build();
        Notification second = Notification.builder().member(member).policy(policy).build();
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        entityManager.clear();

        Notification selected = entityManager.find(Notification.class, first.getNotificationId());
        selected.markAsRead();
        entityManager.flush();
        entityManager.clear();

        Notification stored = entityManager.find(Notification.class, first.getNotificationId());
        assertThat(stored.getReadYn()).isTrue();
        stored.markAsRead();
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Notification.class, first.getNotificationId()).getReadYn()).isTrue();
        assertThat(entityManager.find(Notification.class, second.getNotificationId()).getReadYn()).isFalse();
    }

    @Test
    void deletingNotificationDoesNotDeleteMemberOrPolicy() {
        Notification notification = Notification.builder().member(member).policy(policy).build();
        entityManager.persist(notification);
        entityManager.flush();
        Long memberId = member.getMemberId();
        entityManager.remove(notification);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Notification.class, notification.getNotificationId())).isNull();
        assertThat(entityManager.find(Member.class, memberId)).isNotNull();
        assertThat(entityManager.find(Policy.class, "policy-1")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "policy"})
    void databaseRejectsMissingReferencedEntity(String missing) {
        Long memberId = missing.equals("member") ? -1L : member.getMemberId();
        String policyId = missing.equals("policy") ? "missing-policy" : "policy-1";
        String constraint = missing.equals("member") ? "FK_NOTIFICATION_MEMBER" : "FK_NOTIFICATION_POLICY";

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO notification (member_id, policy_id, read_yn, created_at, updated_at)
                VALUES (:memberId, :policyId, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter("memberId", memberId)
                .setParameter("policyId", policyId)
                .executeUpdate())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(constraint);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "policy", "read"})
    void databaseRejectsNullRequiredField(String missing) {
        Long memberId = missing.equals("member") ? null : member.getMemberId();
        String policyId = missing.equals("policy") ? null : "policy-1";
        Boolean readYn = missing.equals("read") ? null : false;

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO notification (member_id, policy_id, read_yn, created_at, updated_at)
                VALUES (:memberId, :policyId, :readYn, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter("memberId", memberId)
                .setParameter("policyId", policyId)
                .setParameter("readYn", readYn)
                .executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }
}
