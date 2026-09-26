package com.hyeja.domain.profile.ctrl;

import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.ExceptionAdvice;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest {

    private final ProfileService profileService = mock(ProfileService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProfileController(profileService))
                .setControllerAdvice(new ExceptionAdvice()).build();
    }

    @Test
    void returnsMyProfile() throws Exception {
        when(profileService.getMyProfile(1L)).thenReturn(ProfileResponseDTO.builder()
                .birth(LocalDate.of(2000, 3, 15))
                .age(26)
                .regionCode("11440")
                .regionName("서울특별시 마포구")
                .employmentCode(EmploymentStatus.EMPLOYED)
                .employmentName("재직자")
                .houselessYn(true)
                .housingType(HousingType.MONTHLY_RENT)
                .housingTypeName("월세")
                .updatedAt(LocalDateTime.of(2026, 9, 22, 14, 10, 2))
                .build());

        mvc.perform(get("/api/members/me/profile").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.birth").value("2000-03-15"))
                .andExpect(jsonPath("$.result.age").value(26))
                .andExpect(jsonPath("$.result.regionName").value("서울특별시 마포구"))
                .andExpect(jsonPath("$.result.employmentCode").value("EMPLOYED"))
                .andExpect(jsonPath("$.result.employmentName").value("재직자"))
                .andExpect(jsonPath("$.result.housingType").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.updatedAt").value("2026-09-22T14:10:02"))
                // 등록하지 않은 선택 항목은 필드가 빠지지 않고 null로 내려갑니다.
                .andExpect(jsonPath("$.result.marriageCode").value(nullValue()))
                .andExpect(jsonPath("$.result.marriageName").value(nullValue()));
    }

    @Test
    void returnsNotFoundWhenProfileIsNotRegistered() throws Exception {
        when(profileService.getMyProfile(1L)).thenThrow(new GeneralException(ErrorStatus.PROFILE_NOT_FOUND));

        mvc.perform(get("/api/members/me/profile").param("memberId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("PROFILE_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void returnsBadRequestWithoutMemberId() throws Exception {
        mvc.perform(get("/api/members/me/profile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    // 수정 요청은 조건 8개를 전부 보냅니다(회원가입의 profile과 같은 필드, 중첩 없이 바로). 혼인·소득·학력은 null로 비움.
    private static final String UPDATE_BODY = """
            {
              "birth": "2000-03-15",
              "regionCode": "41111",
              "employmentCode": "UNEMPLOYED",
              "houselessYn": true,
              "marriageCode": null,
              "incomeRangeCode": null,
              "educationCode": null,
              "housingType": "JEONSE"
            }
            """;

    @Test
    void updatesMyProfile() throws Exception {
        when(profileService.updateMyProfile(eq(1L), any())).thenReturn(ProfileResponseDTO.builder()
                .birth(LocalDate.of(2000, 3, 15))
                .age(26)
                .regionCode("41111")
                .regionName("경기도 수원시 장안구")
                .employmentCode(EmploymentStatus.UNEMPLOYED)
                .employmentName("미취업자")
                .houselessYn(true)
                .housingType(HousingType.JEONSE)
                .housingTypeName("전세")
                .updatedAt(LocalDateTime.of(2026, 9, 26, 15, 0, 0))
                .build());

        mvc.perform(updateRequest(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.regionName").value("경기도 수원시 장안구"))
                .andExpect(jsonPath("$.result.employmentName").value("미취업자"))
                .andExpect(jsonPath("$.result.updatedAt").value("2026-09-26T15:00:00"))
                .andExpect(jsonPath("$.result.marriageCode").value(nullValue()));
    }

    // 필수 항목 누락은 @Valid 검증에 걸려 COMMON_003이고, result에 필드별 메시지가 담깁니다.
    @Test
    void rejectsUpdateWithoutRequiredField() throws Exception {
        mvc.perform(updateRequest(UPDATE_BODY.replace("\"regionCode\": \"41111\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.result.regionCode").exists());
        verifyNoInteractions(profileService);
    }

    @Test
    void returnsNotFoundWhenRegionDoesNotExist() throws Exception {
        when(profileService.updateMyProfile(eq(1L), any()))
                .thenThrow(new GeneralException(ErrorStatus.REGION_NOT_FOUND));

        mvc.perform(updateRequest(UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGION_001"));
    }

    private RequestBuilder updateRequest(String body) {
        return patch("/api/members/me/profile").param("memberId", "1")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
