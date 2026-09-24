package com.hyeja.domain.profile.dto;

import com.hyeja.domain.profile.enums.EducationLevel;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.profile.enums.MaritalStatus;
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
public class ProfileResponseDTO {

    private LocalDate birth;

    // 만 나이 (저장하지 않고 birth로 매번 계산)
    private Integer age;

    // 시군구코드 5자리와 지역명 (예: 11440 / 서울특별시 마포구)
    private String regionCode;
    private String regionName;

    private EmploymentStatus employmentCode;
    private String employmentName;

    private Boolean houselessYn;

    private MaritalStatus marriageCode;
    private String marriageName;

    private IncomeRange incomeRangeCode;
    private String incomeRangeName;

    private EducationLevel educationCode;
    private String educationName;

    private HousingType housingType;
    private String housingTypeName;

    // 조건 마지막 수정 시각 (BaseEntity의 updated_at)
    private LocalDateTime updatedAt;
}
