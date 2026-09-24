package com.hyeja.domain.profile.entity;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.profile.converter.IncomeRangeConverter;
import com.hyeja.domain.profile.enums.EducationLevel;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.profile.enums.MaritalStatus;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseEntity {

    @Id
    @Column(name = "email", nullable = false, length = 100, updatable = false)
    private String email;

    // PK와 FK가 같은 컬럼인 @OneToOne은 Hibernate가 회원의 PK를 참조하는 것으로 해석합니다.
    // 이메일 참조에는 @ManyToOne을 사용하되, profile.email의 PK 제약으로 실제 관계는 1:1입니다.
    // email 필드가 컬럼을 저장하므로 연관관계에서는 같은 컬럼을 중복 저장하지 않습니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email", referencedColumnName = "email",
            nullable = false, unique = true, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_profile_member_email"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_code", referencedColumnName = "region_code", nullable = false,
            foreignKey = @ForeignKey(name = "fk_profile_region"))
    private Region region;

    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    // 기존 VARCHAR 컬럼을 유지하면서 enum 이름을 저장합니다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "employment_code", nullable = false, length = 20)
    private EmploymentStatus employmentCode;

    @Column(name = "houseless_yn", nullable = false)
    private Boolean houselessYn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "marriage_code", length = 10)
    private MaritalStatus marriageCode;

    @Convert(converter = IncomeRangeConverter.class)
    @Column(name = "income_range_code", length = 10)
    private IncomeRange incomeRangeCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "education_code", length = 30)
    private EducationLevel educationCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "housing_type", length = 20)
    private HousingType housingType;

    @Builder
    public Profile(Member member, Region region, LocalDate birth, EmploymentStatus employmentCode,
            Boolean houselessYn, MaritalStatus marriageCode, IncomeRange incomeRangeCode,
            EducationLevel educationCode, HousingType housingType) {
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.email = Objects.requireNonNull(member.getEmail(), "회원 이메일은 필수입니다.");
        this.region = Objects.requireNonNull(region, "거주 지역은 필수입니다.");
        this.birth = birth;
        this.employmentCode = employmentCode;
        this.houselessYn = houselessYn;
        this.marriageCode = marriageCode;
        this.incomeRangeCode = incomeRangeCode;
        this.educationCode = educationCode;
        this.housingType = housingType;
    }
}
