package com.hyeja.domain.profile.entity;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ProfileTest {

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUpRegion() {
        entityManager.persist(Region.builder().regionCode("11110").sigunguName("종로구").build());
        entityManager.flush();
    }

    @Test
    void persistsProfileAndReferencesMemberByEmail() {
        // 프로필 이메일은 MEMBER.email과 같은 최대 길이(100)를 지원합니다.
        String email = "a".repeat(88) + "@example.com";
        Member member = persistMember(email);
        Profile profile = newProfile(member)
                .marriageCode("UNMARRIED")
                .incomeRangeCode("TEST")
                .educationCode("TEST")
                .housingType("TEST")
                .build();
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();

        Profile stored = entityManager.find(Profile.class, email);
        assertThat(stored.getEmail()).isEqualTo(email);
        assertThat(stored.getMember().getMemberId()).isEqualTo(member.getMemberId());
        assertThat(stored.getMember().getEmail()).isEqualTo(email);
        assertThat(stored.getRegion().getRegionCode()).isEqualTo("11110");
        assertThat(stored.getRegion().getSigunguName()).isEqualTo("종로구");
        assertThat(stored.getBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(stored.getEmploymentCode()).isEqualTo("TEST");
        assertThat(stored.getHouselessYn()).isTrue();
        assertThat(stored.getMarriageCode()).isEqualTo("UNMARRIED");
        assertThat(stored.getIncomeRangeCode()).isEqualTo("TEST");
        assertThat(stored.getEducationCode()).isEqualTo("TEST");
        assertThat(stored.getHousingType()).isEqualTo("TEST");
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    void allowsEmptyOptionalFieldsAndFalseHouselessValue() {
        Member member = persistMember("optional@example.com");
        entityManager.persist(newProfile(member).houselessYn(false).build());
        entityManager.flush();
        entityManager.clear();

        Profile stored = entityManager.find(Profile.class, member.getEmail());
        assertThat(stored.getHouselessYn()).isFalse();
        assertThat(stored.getMarriageCode()).isNull();
        assertThat(stored.getIncomeRangeCode()).isNull();
        assertThat(stored.getEducationCode()).isNull();
        assertThat(stored.getHousingType()).isNull();
    }

    @Test
    void rejectsSecondProfileForSameMember() {
        Member member = persistMember("duplicate@example.com");
        entityManager.persist(newProfile(member).build());
        entityManager.flush();
        entityManager.clear();
        Member storedMember = entityManager.find(Member.class, member.getMemberId());

        assertThatThrownBy(() -> {
            entityManager.persist(newProfile(storedMember).build());
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsEmailWithoutMember() {
        // 엔티티 생성 검증을 우회해 실제 DB 외래키가 적용되는지 확인합니다.
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO profile
                    (email, region_code, birth, employment_code, houseless_yn, created_at, updated_at)
                VALUES
                    ('missing@example.com', '11110', '2000-01-01', 'TEST', true,
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsMissingRequiredProfileField() {
        persistMember("required@example.com");
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO profile
                    (email, region_code, birth, employment_code, houseless_yn, created_at, updated_at)
                VALUES
                    ('required@example.com', '11110', '2000-01-01', 'TEST', NULL,
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void multipleProfilesCanReferenceSameRegion() {
        Member firstMember = persistMember("first@example.com");
        Member secondMember = persistMember("second@example.com");
        entityManager.persist(newProfile(firstMember).build());
        entityManager.persist(newProfile(secondMember).build());
        entityManager.flush();
        entityManager.clear();

        Profile first = entityManager.find(Profile.class, firstMember.getEmail());
        Profile second = entityManager.find(Profile.class, secondMember.getEmail());
        assertThat(first.getRegion().getRegionCode()).isEqualTo("11110");
        assertThat(second.getRegion().getRegionCode()).isEqualTo("11110");
        assertThat(entityManager.createQuery("select count(r) from Region r", Long.class)
                .getSingleResult()).isEqualTo(1L);
    }

    @Test
    void regionIsLoadedOnlyWhenAccessed() {
        Member member = persistMember("lazy@example.com");
        entityManager.persist(newProfile(member).build());
        entityManager.flush();
        entityManager.clear();

        Profile stored = entityManager.find(Profile.class, member.getEmail());
        var persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(stored, "region")).isFalse();
        assertThat(stored.getRegion().getSigunguName()).isEqualTo("종로구");
        assertThat(persistenceUnitUtil.isLoaded(stored, "region")).isTrue();
    }

    @Test
    void databaseRejectsRegionWithoutMatchingRow() {
        persistMember("unknown-region@example.com");
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO profile
                    (email, region_code, birth, employment_code, houseless_yn, created_at, updated_at)
                VALUES
                    ('unknown-region@example.com', '99999', '2000-01-01', 'TEST', true,
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("FK_PROFILE_REGION");
    }

    private Member persistMember(String email) {
        Member member = Member.builder()
                .email(email).password("encoded-password").nickname("혜자").build();
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    private Profile.ProfileBuilder newProfile(Member member) {
        return Profile.builder()
                .member(member)
                .region(entityManager.getReference(Region.class, "11110"))
                .birth(LocalDate.of(2000, 1, 1))
                .employmentCode("TEST")
                .houselessYn(true);
    }
}
