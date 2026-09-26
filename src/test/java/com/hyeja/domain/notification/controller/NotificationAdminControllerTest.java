package com.hyeja.domain.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.notification.service.NotificationGenerationService;
import com.hyeja.global.config.SchedulingConfig;
import com.hyeja.global.exception.ExceptionAdvice;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationAdminControllerTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 9, 26);

    @Mock
    private NotificationGenerationService notificationGenerationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ZoneId koreaZone = ZoneId.of(SchedulingConfig.KOREA_TIME_ZONE);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(BASE_DATE, LocalTime.NOON, koreaZone).toInstant(),
                koreaZone
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationAdminController(
                        notificationGenerationService,
                        clock
                ))
                .setControllerAdvice(new ExceptionAdvice())
                .build();
    }

    @Test
    void generatesDeadlineNotificationsOnlyForRequestedMember() throws Exception {
        when(notificationGenerationService.createDeadlineNotificationsForMember(BASE_DATE, 1L))
                .thenReturn(2);

        mockMvc.perform(post("/api/notification/admin/generate")
                        .param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result").value(2));

        verify(notificationGenerationService)
                .createDeadlineNotificationsForMember(BASE_DATE, 1L);
    }

    @Test
    void returnsBadRequestWithoutMemberId() throws Exception {
        mockMvc.perform(post("/api/notification/admin/generate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsBadRequestWhenMemberIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/notification/admin/generate")
                        .param("memberId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
