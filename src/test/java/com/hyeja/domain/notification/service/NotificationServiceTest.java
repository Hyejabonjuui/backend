package com.hyeja.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void hardDeletesOwnedNotification() {
        Member member = member(1L);
        Notification notification = notification(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationRepository.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 10L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void throwsMemberNotFoundWhenMemberDoesNotExist() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(99L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void throwsNotificationNotFoundWhenNotificationIsMissingOrNotOwned() {
        Member member = member(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationRepository.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND);
    }

    private Member member(Long memberId) {
        Member member = Member.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("회원")
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }

    private Notification notification(Long notificationId, Member member) {
        Policy policy = Policy.builder()
                .policyId("policy-1")
                .policyName("테스트 정책")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(false)
                .applyPeriodCode("PERIOD")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .build();
        Notification notification = Notification.builder()
                .member(member)
                .policy(policy)
                .build();
        ReflectionTestUtils.setField(notification, "notificationId", notificationId);
        return notification;
    }
}
