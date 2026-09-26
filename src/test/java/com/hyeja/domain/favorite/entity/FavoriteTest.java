package com.hyeja.domain.favorite.entity;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
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
class FavoriteTest {

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Policy policy;

    @BeforeEach
    void setUp() {
        member = persistMember("member@example.com");
        policy = persistPolicy("policy-1");
        entityManager.flush();
    }

    @Test
    void persistsFavoriteAndLoadsAssociationsLazily() {
        Favorite favorite = Favorite.builder().member(member).policy(policy).build();
        entityManager.persist(favorite);
        entityManager.flush();
        entityManager.clear();

        Favorite stored = entityManager.find(Favorite.class, favorite.getFavoriteId());
        assertThat(stored.getFavoriteId()).isNotNull();
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
    void rejectsDuplicateMemberPolicyPair() {
        entityManager.persist(Favorite.builder().member(member).policy(policy).build());
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.persist(Favorite.builder().member(member).policy(policy).build());
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("UK_FAVORITE_MEMBER_POLICY");
    }

    @Test
    void allowsMultiplePoliciesPerMemberAndMembersPerPolicy() {
        Member otherMember = persistMember("other@example.com");
        Policy otherPolicy = persistPolicy("policy-2");
        entityManager.persist(Favorite.builder().member(member).policy(policy).build());
        entityManager.persist(Favorite.builder().member(member).policy(otherPolicy).build());
        entityManager.persist(Favorite.builder().member(otherMember).policy(policy).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.createQuery(
                "select count(f) from Favorite f where f.member.memberId = :id", Long.class)
                .setParameter("id", member.getMemberId()).getSingleResult()).isEqualTo(2L);
        assertThat(entityManager.createQuery(
                "select count(f) from Favorite f where f.policy.policyId = :id", Long.class)
                .setParameter("id", "policy-1").getSingleResult()).isEqualTo(2L);
    }

    @Test
    void physicalDeletionPreservesMemberAndPolicyAndAllowsRegisteringAgain() {
        Favorite favorite = Favorite.builder().member(member).policy(policy).build();
        entityManager.persist(favorite);
        entityManager.flush();
        Long deletedId = favorite.getFavoriteId();
        Long memberId = member.getMemberId();
        entityManager.remove(favorite);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Favorite.class, deletedId)).isNull();
        Member storedMember = entityManager.find(Member.class, memberId);
        Policy storedPolicy = entityManager.find(Policy.class, "policy-1");
        assertThat(storedMember).isNotNull();
        assertThat(storedPolicy).isNotNull();

        Favorite registeredAgain = Favorite.builder().member(storedMember).policy(storedPolicy).build();
        entityManager.persist(registeredAgain);
        entityManager.flush();
        entityManager.clear();

        assertThat(registeredAgain.getFavoriteId()).isNotEqualTo(deletedId);
        assertThat(entityManager.find(Favorite.class, registeredAgain.getFavoriteId())).isNotNull();
        assertThat(entityManager.createQuery("select count(f) from Favorite f", Long.class)
                .getSingleResult()).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "policy"})
    void databaseRejectsMissingReferencedEntity(String missing) {
        Long memberId = missing.equals("member") ? -1L : member.getMemberId();
        String policyId = missing.equals("policy") ? "missing-policy" : "policy-1";
        String constraint = missing.equals("member") ? "FK_FAVORITE_MEMBER" : "FK_FAVORITE_POLICY";

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO favorite (member_id, policy_id, created_at, updated_at)
                VALUES (:memberId, :policyId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter("memberId", memberId)
                .setParameter("policyId", policyId)
                .executeUpdate())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining(constraint);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "policy"})
    void databaseRejectsNullAssociation(String missing) {
        Long memberId = missing.equals("member") ? null : member.getMemberId();
        String policyId = missing.equals("policy") ? null : "policy-1";

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO favorite (member_id, policy_id, created_at, updated_at)
                VALUES (:memberId, :policyId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter("memberId", memberId)
                .setParameter("policyId", policyId)
                .executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Member persistMember(String email) {
        Member value = Member.builder().email(email).password("encoded-password").nickname("회원").build();
        entityManager.persist(value);
        return value;
    }

    private Policy persistPolicy(String id) {
        Policy value = Policy.builder().policyId(id).policyName("테스트 정책")
                .category(PolicyCategory.OTHER).ageLimitYn(false).applyPeriodCode("TEST").build();
        entityManager.persist(value);
        return value;
    }
}
