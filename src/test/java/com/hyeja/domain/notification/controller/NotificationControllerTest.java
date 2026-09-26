package com.hyeja.domain.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationItemDTO;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationListDTO;
import com.hyeja.domain.notification.service.NotificationService;
import com.hyeja.global.exception.ExceptionAdvice;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new ExceptionAdvice())
                .build();
        loginAs(1L);
    }

    // 토큰 대신 "1번 회원으로 로그인한 상태"를 직접 만들어, @AuthenticationPrincipal Long memberId에 1이 들어가게 합니다.
    // (실제 토큰 검증은 SecurityTest에서 확인합니다.)
    private static void loginAs(Long memberId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, "access-token", List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNotificationsForMember() throws Exception {
        NotificationItemDTO response = NotificationItemDTO.builder()
                .notificationId(10L)
                .memberId(1L)
                .policyId("policy-1")
                .readYn(false)
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        NotificationListDTO pageResponse = NotificationListDTO.builder()
                .notifications(List.of(response))
                .page(0)
                .size(8)
                .totalElements(9)
                .totalPages(2)
                .hasNext(true)
                .build();
        when(notificationService.getNotifications(1L, 0, 8)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/notification")
                        .param("page", "0")
                        .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.notifications[0].notification_id").value(10))
                .andExpect(jsonPath("$.result.notifications[0].member_id").value(1))
                .andExpect(jsonPath("$.result.notifications[0].policy_id").value("policy-1"))
                .andExpect(jsonPath("$.result.notifications[0].read_yn").value(false))
                .andExpect(jsonPath("$.result.notifications[0].apply_end_date").value("2026-09-30"))
                .andExpect(jsonPath("$.result.notifications[0].created_at").value("2026-09-24T10:30:00"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(8))
                .andExpect(jsonPath("$.result.totalElements").value(9))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true));

        verify(notificationService).getNotifications(1L, 0, 8);
    }

    @Test
    void marksNotificationAsReadForMember() throws Exception {
        NotificationItemDTO response = NotificationItemDTO.builder()
                .notificationId(10L)
                .memberId(1L)
                .policyId("policy-1")
                .readYn(true)
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        when(notificationService.markAsRead(1L, 10L)).thenReturn(response);

        mockMvc.perform(patch("/api/notification/{notificationId}/read", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result.notification_id").value(10))
                .andExpect(jsonPath("$.result.member_id").value(1))
                .andExpect(jsonPath("$.result.policy_id").value("policy-1"))
                .andExpect(jsonPath("$.result.read_yn").value(true))
                .andExpect(jsonPath("$.result.apply_end_date").value("2026-09-30"))
                .andExpect(jsonPath("$.result.created_at").value("2026-09-24T10:30:00"));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    void deletesNotificationForMember() throws Exception {
        mockMvc.perform(delete("/api/notification/{notificationId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(notificationService).deleteNotification(1L, 10L);
    }
}
