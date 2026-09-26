package com.hyeja.domain.member.controller;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.ExceptionAdvice;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // 계정 정보 + 내 조건. 조건의 선택 항목(혼인·소득·학력)은 null로 비워서 보냅니다.
    private static final String SIGNUP_BODY = """
            {
              "email": "hyeja@example.com",
              "password": "hyeja1234!",
              "nickname": "민지",
              "profile": {
                "birth": "2000-03-15",
                "regionCode": "11440",
                "employmentCode": "EMPLOYED",
                "houselessYn": true,
                "marriageCode": null,
                "incomeRangeCode": null,
                "educationCode": null,
                "housingType": "MONTHLY_RENT"
              }
            }
            """;

    @Test
    void signsUpWithSuccess() throws Exception {
        when(memberService.signup(any())).thenReturn(MemberAccountResponseDTO.builder()
                .memberId(1L)
                .email("hyeja@example.com")
                .nickname("민지")
                .createdAt(LocalDateTime.of(2026, 9, 25, 14, 3, 11))
                .build());

        mvc.perform(signupRequest(SIGNUP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.memberId").value(1))
                .andExpect(jsonPath("$.result.email").value("hyeja@example.com"))
                // 비밀번호는 응답에 담지 않습니다.
                .andExpect(jsonPath("$.result.password").doesNotExist());
    }

    @Test
    void returnsBadRequestWhenMemberIdIsNotPositive() throws Exception {
        mvc.perform(get("/api/members/me").param("memberId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    // 계정 필드와 profile 안쪽 필드가 함께 검증됩니다. profile 안쪽 오류는 "profile.필드명"으로 내려갑니다.
    @Test
    void rejectsInvalidAccountAndProfileFields() throws Exception {
        String body = SIGNUP_BODY
                .replace("hyeja1234!", "short")
                .replace("\"birth\": \"2000-03-15\"", "\"birth\": \"2999-01-01\"");

        mvc.perform(signupRequest(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.result.password").exists())
                .andExpect(jsonPath("$.result['profile.birth']").exists());
        verifyNoInteractions(memberService);
    }

    // 비밀번호는 영문·숫자·특수문자를 모두 포함해야 합니다. 하나라도 빠지면 400입니다.
    @Test
    void rejectsPasswordWithoutLetterDigitOrSpecialChar() throws Exception {
        for (String password : new String[] {"hyeja12345", "hyejahyeja!", "12345678!"}) {
            mvc.perform(signupRequest(SIGNUP_BODY.replace("hyeja1234!", password)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_003"))
                    .andExpect(jsonPath("$.result.password").exists());
        }
        verifyNoInteractions(memberService);
    }

    // 조건 없이는 가입할 수 없습니다.
    @Test
    void rejectsSignupWithoutProfile() throws Exception {
        String body = """
                {"email": "hyeja@example.com", "password": "hyeja1234!", "nickname": "민지"}
                """;

        mvc.perform(signupRequest(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.result.profile").exists());
        verifyNoInteractions(memberService);
    }

    // 목록에 없는 코드는 JSON → enum 변환 단계에서 실패하므로 검증(COMMON_003)이 아니라 COMMON_001입니다.
    @Test
    void rejectsUnknownProfileCode() throws Exception {
        mvc.perform(signupRequest(SIGNUP_BODY.replace("EMPLOYED", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
        verifyNoInteractions(memberService);
    }

    @Test
    void returnsConflictWhenEmailIsDuplicated() throws Exception {
        when(memberService.signup(any())).thenThrow(new GeneralException(ErrorStatus.MEMBER_EMAIL_DUPLICATED));

        mvc.perform(signupRequest(SIGNUP_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_002"));
    }

    private RequestBuilder signupRequest(String body) {
        return post("/api/members").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
