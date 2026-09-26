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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void returnsBadRequestWhenMemberIdIsNotPositive() throws Exception {
        mvc.perform(get("/api/members/me/profile").param("memberId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
