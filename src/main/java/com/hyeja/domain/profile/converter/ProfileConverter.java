package com.hyeja.domain.profile.converter;

import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.entity.Profile;
import java.time.LocalDate;
import java.time.Period;

// 프로필 엔티티 → 응답 DTO 변환을 모아 둡니다. 서비스는 조회·검증만 맡고 변환은 여기서 합니다.
// (같은 패키지의 IncomeRangeConverter는 JPA AttributeConverter로, 용도가 다릅니다.)
public class ProfileConverter {

    // 내 조건 조회 응답으로 변환합니다. 지역(LAZY)을 읽으므로 트랜잭션 안에서 호출해야 합니다.
    public static ProfileResponseDTO toProfileResponseDTO(Profile profile) {
        return ProfileResponseDTO.builder()
                .birth(profile.getBirth())
                .age(Period.between(profile.getBirth(), LocalDate.now()).getYears())
                .regionCode(profile.getRegion().getRegionCode())
                .regionName(profile.getRegion().getSigunguName())
                .employmentCode(profile.getEmploymentCode())
                .employmentName(profile.getEmploymentCode().getLabel())
                .houselessYn(profile.getHouselessYn())
                // 아래는 선택 항목이라 등록하지 않았으면 이름도 null로 둡니다.
                .marriageCode(profile.getMarriageCode())
                .marriageName(profile.getMarriageCode() == null ? null : profile.getMarriageCode().getLabel())
                .incomeRangeCode(profile.getIncomeRangeCode())
                .incomeRangeName(profile.getIncomeRangeCode() == null ? null : profile.getIncomeRangeCode().getLabel())
                .educationCode(profile.getEducationCode())
                .educationName(profile.getEducationCode() == null ? null : profile.getEducationCode().getLabel())
                .housingType(profile.getHousingType())
                .housingTypeName(profile.getHousingType() == null ? null : profile.getHousingType().getLabel())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
