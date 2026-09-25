package com.hyeja.domain.profile.dto;

import com.hyeja.domain.profile.enums.EducationLevel;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.profile.enums.MaritalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 내 조건 입력값입니다. 회원가입 요청의 profile 필드로 받습니다(조건 없이는 가입할 수 없음).
// 선택 항목(혼인·소득·학력·주거 형태)은 비워서(null) 보낼 수 있습니다.
// 잘못된 입력은 두 단계에서 400으로 막힙니다.
//  - 목록에 없는 코드(예: "STUDENT"): JSON → enum 변환이 실패해 COMMON_001
//  - 필수 항목 누락·미래 생년월일: 아래 검증 어노테이션(@Valid)에 걸려 COMMON_003, result에 필드별 메시지
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ProfileRequestDTO", description = "내 조건 입력값")
public class ProfileRequestDTO {

    @Schema(description = "생년월일", example = "2000-03-15")
    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 오늘 이전이어야 합니다.")
    private LocalDate birth;

    @Schema(description = "거주지 시군구코드 5자리", example = "11440")
    @NotBlank(message = "거주지는 필수입니다.")
    private String regionCode;

    @Schema(description = "취업 상태 코드", example = "EMPLOYED")
    @NotNull(message = "취업 상태는 필수입니다.")
    private EmploymentStatus employmentCode;

    @Schema(description = "무주택 여부", example = "true")
    @NotNull(message = "무주택 여부는 필수입니다.")
    private Boolean houselessYn;

    @Schema(description = "혼인 여부 코드", example = "SINGLE", nullable = true)
    private MaritalStatus marriageCode;

    @Schema(description = "연소득 구간 코드", example = "R2000_3000", nullable = true)
    private IncomeRange incomeRangeCode;

    @Schema(description = "학력 코드", example = "COLLEGE_GRADUATE", nullable = true)
    private EducationLevel educationCode;

    @Schema(description = "주거 형태 코드", example = "MONTHLY_RENT", nullable = true)
    private HousingType housingType;
}
