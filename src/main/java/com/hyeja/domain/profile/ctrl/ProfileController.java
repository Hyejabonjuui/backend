package com.hyeja.domain.profile.ctrl;

import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// URL은 명세대로 /members/me/profile 이지만, 다루는 데이터가 Profile이라 profile 도메인에 둡니다.
@RestController
@RequestMapping("/api/members/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 내 조건 조회 (마이페이지 S-08 내 조건 탭, 추천 시 자동 로드) — 예: GET /api/members/me/profile?memberId=1
    // TODO: 인증(JWT) 기반이 생기면 memberId 쿼리 파라미터를 없애고 토큰에서 회원을 식별합니다.
    //       그 전까지는 memberId만 알면 누구의 조건이든 조회되므로 임시 방식입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getMyProfile(@RequestParam Long memberId) {
        ProfileResponseDTO result = profileService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
