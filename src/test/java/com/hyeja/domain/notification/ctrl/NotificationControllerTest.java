package com.hyeja.domain.notification.ctrl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.notification.controller.NotificationController;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationItemDTO;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationListDTO;
import com.hyeja.domain.notification.service.NotificationService;
import com.hyeja.global.exception.ExceptionAdvice;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                .setControllerAdvice(new ExceptionAdvice())
                .build();
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

        mockMvc.perform(get("/api/notification/{memberId}", 1L)
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
}
