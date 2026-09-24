package com.hyeja.domain.member.ctrl;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.ExceptionAdvice;
import com.hyeja.global.exception.GeneralException;
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

class MemberControllerTest {

    private final MemberService memberService = mock(MemberService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService))
                .setControllerAdvice(new ExceptionAdvice()).build();
    }

    @Test
    void returnsMyAccount() throws Exception {
        when(memberService.getMyAccount(1L)).thenReturn(MemberAccountResponseDTO.builder()
                .memberId(1L)
                .email("hyeja@example.com")
                .nickname("민지")
                .createdAt(LocalDateTime.of(2026, 9, 22, 14, 3, 11))
                .build());

        mvc.perform(get("/api/members/me").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.memberId").value(1))
                .andExpect(jsonPath("$.result.email").value("hyeja@example.com"))
                .andExpect(jsonPath("$.result.nickname").value("민지"))
                .andExpect(jsonPath("$.result.createdAt").value("2026-09-22T14:03:11"));
    }

    @Test
    void returnsNotFoundWhenMemberDoesNotExist() throws Exception {
        when(memberService.getMyAccount(99L)).thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        mvc.perform(get("/api/members/me").param("memberId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void returnsBadRequestWithoutMemberId() throws Exception {
        mvc.perform(get("/api/members/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
