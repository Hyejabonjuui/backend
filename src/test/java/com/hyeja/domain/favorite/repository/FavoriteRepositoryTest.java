package com.hyeja.domain.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.member.entity.Member;
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
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsOnlyActiveFavoritesForMemberInRecentOrderWithPolicyFetched() {
        Member member = persistMember("member@example.com");
        Member otherMember = persistMember("other@example.com");
        Policy firstPolicy = persistPolicy("policy-1", "첫 번째 정책");
        Policy secondPolicy = persistPolicy("policy-2", "두 번째 정책");
        Policy deletedPolicy = persistPolicy("policy-3", "삭제된 관심 정책");

        Favorite first = Favorite.builder().member(member).policy(firstPolicy).build();
        Favorite second = Favorite.builder().member(member).policy(secondPolicy).build();
        Favorite deleted = Favorite.builder().member(member).policy(deletedPolicy).build();
        Favorite other = Favorite.builder().member(otherMember).policy(firstPolicy).build();
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(deleted);
        entityManager.persist(other);
        deleted.softDelete();
        entityManager.flush();
        entityManager.clear();

        var result = favoriteRepository.findAllActiveByMemberId(
                member.getMemberId(),
                PageRequest.of(0, 8)
        );

        assertThat(result.getContent()).extracting(favorite -> favorite.getPolicy().getPolicyId())
                .containsExactly("policy-2", "policy-1");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        var persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(result.getContent()).allSatisfy(favorite ->
                assertThat(persistenceUnitUtil.isLoaded(favorite, "policy")).isTrue());
    }

    @Test
    void detectsFavoriteAndAllowsRegistrationAfterHardDelete() {
        Member member = persistMember("register@example.com");
        Policy policy = persistPolicy("register-policy", "등록할 정책");
        Favorite favorite = favoriteRepository.saveAndFlush(
                Favorite.builder().member(member).policy(policy).build()
        );

        assertThat(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(
                member.getMemberId(), policy.getPolicyId())).isTrue();

        Long deletedId = favorite.getFavoriteId();
        favoriteRepository.delete(favorite);
        favoriteRepository.flush();

        assertThat(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(
                member.getMemberId(), policy.getPolicyId())).isFalse();

        Favorite registeredAgain = favoriteRepository.saveAndFlush(
                Favorite.builder().member(member).policy(policy).build()
        );
        assertThat(registeredAgain.getFavoriteId()).isNotEqualTo(deletedId);
    }

    private Member persistMember(String email) {
        Member member = Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname("회원")
                .build();
        entityManager.persist(member);
        return member;
    }

    private Policy persistPolicy(String policyId, String policyName) {
        Policy policy = Policy.builder()
                .policyId(policyId)
                .policyName(policyName)
                .category(PolicyCategory.MONTHLY_RENT)
                .supportContent("월세를 지원합니다.")
                .ageLimitYn(false)
                .applyPeriodCode("PERIOD")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .build();
        entityManager.persist(policy);
        return policy;
    }
}
