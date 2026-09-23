package com.hyeja.domain.profile.entity;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.profile.enums.EducationLevel;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.MaritalStatus;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
                .marriageCode(MaritalStatus.SINGLE)
                .incomeRangeCode("TEST")
                .educationCode(EducationLevel.COLLEGE_GRADUATE)
                .housingType(HousingType.MONTHLY_RENT)
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
        assertThat(stored.getEmploymentCode()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(stored.getHouselessYn()).isTrue();
        assertThat(stored.getMarriageCode()).isEqualTo(MaritalStatus.SINGLE);
        assertThat(stored.getIncomeRangeCode()).isEqualTo("TEST");
        assertThat(stored.getEducationCode()).isEqualTo(EducationLevel.COLLEGE_GRADUATE);
        assertThat(stored.getHousingType()).isEqualTo(HousingType.MONTHLY_RENT);
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
                    ('missing@example.com', '11110', '2000-01-01', 'EMPLOYED', true,
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
                    ('required@example.com', '11110', '2000-01-01', 'EMPLOYED', NULL,
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
                    ('unknown-region@example.com', '99999', '2000-01-01', 'EMPLOYED', true,
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("FK_PROFILE_REGION");
    }

    @ParameterizedTest
    @EnumSource(EmploymentStatus.class)
    void storesEveryEmploymentStatusByName(EmploymentStatus status) {
        Member member = persistMember("employment@example.com");
        entityManager.persist(newProfile(member).employmentCode(status).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Profile.class, member.getEmail()).getEmploymentCode()).isEqualTo(status);
        assertThat(storedColumn("employment_code", member.getEmail())).isEqualTo(status.name());
    }

    @ParameterizedTest
    @EnumSource(MaritalStatus.class)
    void storesEveryMaritalStatusByName(MaritalStatus status) {
        Member member = persistMember("marital@example.com");
        entityManager.persist(newProfile(member).marriageCode(status).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Profile.class, member.getEmail()).getMarriageCode()).isEqualTo(status);
        assertThat(storedColumn("marriage_code", member.getEmail())).isEqualTo(status.name());
    }

    @ParameterizedTest
    @EnumSource(HousingType.class)
    void storesEveryHousingTypeByName(HousingType type) {
        Member member = persistMember("housing@example.com");
        entityManager.persist(newProfile(member).housingType(type).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Profile.class, member.getEmail()).getHousingType()).isEqualTo(type);
        assertThat(storedColumn("housing_type", member.getEmail())).isEqualTo(type.name());
    }

    @ParameterizedTest
    @EnumSource(EducationLevel.class)
    void storesEveryEducationLevelByName(EducationLevel level) {
        Member member = persistMember("education@example.com");
        entityManager.persist(newProfile(member).educationCode(level).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Profile.class, member.getEmail()).getEducationCode()).isEqualTo(level);
        assertThat(storedColumn("education_code", member.getEmail())).isEqualTo(level.name());
    }

    @Test
    void databaseRejectsMissingEmploymentStatus() {
        persistMember("missing-employment@example.com");
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                INSERT INTO profile
                    (email, region_code, birth, employment_code, houseless_yn, created_at, updated_at)
                VALUES
                    ('missing-employment@example.com', '11110', '2000-01-01', NULL, true,
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Object storedColumn(String column, String email) {
        return entityManager.createNativeQuery("SELECT " + column + " FROM profile WHERE email = :email")
                .setParameter("email", email).getSingleResult();
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
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true);
    }
}
