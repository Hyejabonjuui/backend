package com.hyeja.domain.member.service;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void returnsAccountOfExistingMember() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 22, 14, 3, 11);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, createdAt)));

        MemberAccountResponseDTO result = memberService.getMyAccount(1L);

        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("hyeja@example.com");
        assertThat(result.getNickname()).isEqualTo("민지");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void throwsMemberNotFoundWhenMemberDoesNotExist() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertMemberNotFound(99L);
    }

    @Test
    void throwsMemberNotFoundWhenMemberIsDeleted() {
        Member deleted = member(1L, LocalDateTime.now());
        deleted.softDelete();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertMemberNotFound(1L);
    }

    private void assertMemberNotFound(Long memberId) {
        assertThatThrownBy(() -> memberService.getMyAccount(memberId))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
    }

    // ID·가입일은 DB와 JPA Auditing이 채우는 값이라 테스트에서는 직접 넣습니다.
    private Member member(Long memberId, LocalDateTime createdAt) {
        Member member = Member.builder()
                .email("hyeja@example.com")
                .password("encoded-password")
                .nickname("민지")
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "createdAt", createdAt);
        return member;
    }
}
