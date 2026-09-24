package com.hyeja.domain.member.ctrl;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 내 계정 조회 (마이페이지 S-08 계정 탭) — 예: GET /api/members/me?memberId=1
    // TODO: 인증(JWT) 기반이 생기면 memberId 쿼리 파라미터를 없애고 토큰에서 회원을 식별합니다.
    //       그 전까지는 memberId만 알면 누구의 계정이든 조회되므로 임시 방식입니다.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberAccountResponseDTO>> getMyAccount(@RequestParam Long memberId) {
        MemberAccountResponseDTO result = memberService.getMyAccount(memberId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
