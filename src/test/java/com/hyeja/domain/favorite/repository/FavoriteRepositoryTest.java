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
import org.springframework.test.util.ReflectionTestUtils;

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
    void searchesPolicyNameOrSupportContentAndKeepsInactivePolicies() {
        Member member = persistMember("search@example.com");
        Member otherMember = persistMember("search-other@example.com");
        Policy nameMatch = persistPolicy(
                "name-match", "청년 월세 지원", "임차료를 지원합니다.");
        Policy contentMatch = persistPolicy(
                "content-match", "청년 주거 지원", "월세 보증금을 지원합니다.");
        Policy noMatch = persistPolicy(
                "no-match", "전세 이자 지원", "이자를 지원합니다.");
        Policy deletedMatch = persistPolicy(
                "deleted-match", "삭제된 월세 정책", "월세를 지원합니다.");
        ReflectionTestUtils.setField(contentMatch, "activeYn", false);
        ReflectionTestUtils.setField(contentMatch, "applyEndDate", null);

        Favorite nameFavorite = Favorite.builder().member(member).policy(nameMatch).build();
        Favorite contentFavorite = Favorite.builder().member(member).policy(contentMatch).build();
        Favorite noMatchFavorite = Favorite.builder().member(member).policy(noMatch).build();
        Favorite deletedFavorite = Favorite.builder().member(member).policy(deletedMatch).build();
        Favorite otherFavorite = Favorite.builder().member(otherMember).policy(nameMatch).build();
        entityManager.persist(nameFavorite);
        entityManager.persist(contentFavorite);
        entityManager.persist(noMatchFavorite);
        entityManager.persist(deletedFavorite);
        entityManager.persist(otherFavorite);
        deletedFavorite.softDelete();
        entityManager.flush();
        entityManager.clear();

        var allMatches = favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                member.getMemberId(), "월세", PageRequest.of(0, 8));
        var firstPage = favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                member.getMemberId(), "월세", PageRequest.of(0, 1));

        assertThat(allMatches.getContent())
                .extracting(favorite -> favorite.getPolicy().getPolicyId())
                .containsExactly("content-match", "name-match");
        assertThat(allMatches.getContent().get(0).getPolicy().getActiveYn()).isFalse();
        assertThat(allMatches.getContent().get(0).getPolicy().getApplyEndDate()).isNull();
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void treatsLikeWildcardsAsLiteralCharactersInSearchAndCountQueries() {
        Member member = persistMember("wildcard@example.com");
        Policy percentPolicy = persistPolicy(
                "percent-policy", "50% 할인 정책", "일반 지원");
        Policy underscorePolicy = persistPolicy(
                "underscore-policy", "일반 정책", "code_value 지원");
        Policy noMatchPolicy = persistPolicy(
                "no-match-policy", "50퍼센트 할인", "codeXvalue 지원");
        entityManager.persist(Favorite.builder().member(member).policy(percentPolicy).build());
        entityManager.persist(Favorite.builder().member(member).policy(underscorePolicy).build());
        entityManager.persist(Favorite.builder().member(member).policy(noMatchPolicy).build());
        entityManager.flush();
        entityManager.clear();

        var percentMatches = favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                member.getMemberId(), "!%", PageRequest.of(0, 1));
        var underscoreMatches = favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                member.getMemberId(), "!_", PageRequest.of(0, 1));

        assertThat(percentMatches.getContent())
                .extracting(favorite -> favorite.getPolicy().getPolicyId())
                .containsExactly("percent-policy");
        assertThat(percentMatches.getTotalElements()).isEqualTo(1);
        assertThat(underscoreMatches.getContent())
                .extracting(favorite -> favorite.getPolicy().getPolicyId())
                .containsExactly("underscore-policy");
        assertThat(underscoreMatches.getTotalElements()).isEqualTo(1);
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
        assertThat(favoriteRepository.findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                member.getMemberId(), policy.getPolicyId())).contains(favorite);

        Long deletedId = favorite.getFavoriteId();
        favoriteRepository.delete(favorite);
        favoriteRepository.flush();

        assertThat(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(
                member.getMemberId(), policy.getPolicyId())).isFalse();
        assertThat(favoriteRepository.findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                member.getMemberId(), policy.getPolicyId())).isEmpty();

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
        return persistPolicy(policyId, policyName, "월세를 지원합니다.");
    }

    private Policy persistPolicy(String policyId, String policyName, String supportContent) {
        Policy policy = Policy.builder()
                .policyId(policyId)
                .policyName(policyName)
                .category(PolicyCategory.MONTHLY_RENT)
                .supportContent(supportContent)
                .ageLimitYn(false)
                .applyPeriodCode("0057003")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .build();
        entityManager.persist(policy);
        return policy;
    }
}
