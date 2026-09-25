package com.hyeja.domain.profile.dto;

import com.hyeja.domain.profile.enums.EducationLevel;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.profile.enums.MaritalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 마이페이지(S-08) 내 조건 탭·추천 화면에서 쓰는 내 조건 응답입니다.
// 코드(enum 이름)와 한글 이름을 같이 내려줍니다. 화면은 이름을 보여주고, 수정 폼은 코드로 선택 상태를 맞춥니다.
// 선택 항목(혼인·소득·학력·주거 형태)을 등록하지 않았으면 코드와 이름 모두 null입니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ProfileResponseDTO", description = "내 조건 조회 응답")
public class ProfileResponseDTO {

    @Schema(description = "생년월일", example = "2000-03-15")
    private LocalDate birth;

    // 만 나이 (저장하지 않고 birth로 매번 계산)
    @Schema(description = "만 나이", example = "26")
    private Integer age;

    // 시군구코드 5자리와 지역명 (예: 11440 / 서울특별시 마포구)
    @Schema(description = "거주지 시군구코드 5자리", example = "11440")
    private String regionCode;
    @Schema(description = "거주지 이름", example = "서울특별시 마포구")
    private String regionName;

    @Schema(description = "취업 상태 코드", example = "EMPLOYED")
    private EmploymentStatus employmentCode;
    @Schema(description = "취업 상태 이름", example = "재직자")
    private String employmentName;

    @Schema(description = "무주택 여부", example = "true")
    private Boolean houselessYn;

    @Schema(description = "혼인 여부 코드", example = "SINGLE", nullable = true)
    private MaritalStatus marriageCode;
    @Schema(description = "혼인 여부 이름", example = "미혼", nullable = true)
    private String marriageName;

    @Schema(description = "연소득 구간 코드", example = "R2000_3000", nullable = true)
    private IncomeRange incomeRangeCode;
    @Schema(description = "연소득 구간 이름", example = "2천만원 이상 3천만원 미만", nullable = true)
    private String incomeRangeName;

    @Schema(description = "학력 코드", example = "COLLEGE_GRADUATE", nullable = true)
    private EducationLevel educationCode;
    @Schema(description = "학력 이름", example = "대학 졸업", nullable = true)
    private String educationName;

    @Schema(description = "주거 형태 코드", example = "MONTHLY_RENT", nullable = true)
    private HousingType housingType;
    @Schema(description = "주거 형태 이름", example = "월세", nullable = true)
    private String housingTypeName;

    // 조건 마지막 수정 시각 (BaseEntity의 updated_at)
    @Schema(description = "조건 마지막 수정 시각", example = "2026-09-24T10:30:00")
    private LocalDateTime updatedAt;
}
