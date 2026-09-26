package com.hyeja.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationItemDTO;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void marksOwnedNotificationAsReadAndReturnsConvertedResponse() {
        Member member = member(1L);
        Notification notification = notification(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationRepository.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(notification));

        NotificationItemDTO result = notificationService.markAsRead(1L, 10L);

        assertThat(notification.getReadYn()).isTrue();
        assertThat(result.getNotificationId()).isEqualTo(10L);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getPolicyId()).isEqualTo("policy-1");
        assertThat(result.getReadYn()).isTrue();
        assertThat(result.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        verify(notificationRepository)
                .findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L);
    }

    @Test
    void keepsAlreadyReadNotificationAsRead() {
        Member member = member(1L);
        Notification notification = notification(10L, member);
        ReflectionTestUtils.setField(notification, "readYn", true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationRepository.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(notification));

        NotificationItemDTO result = notificationService.markAsRead(1L, 10L);

        assertThat(notification.getReadYn()).isTrue();
        assertThat(result.getReadYn()).isTrue();
    }

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
    void throwsMemberNotFoundWhenMarkingAsRead() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void throwsNotificationNotFoundWhenMarkingAsRead() {
        Member member = member(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationRepository.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void throwsMemberNotFoundWhenDeletingNotification() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(99L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void throwsNotificationNotFoundWhenDeletingNotification() {
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
                .applyEndDate(LocalDate.of(2026, 10, 1))
                .build();
        Notification notification = Notification.builder()
                .member(member)
                .policy(policy)
                .deadlineDate(LocalDate.of(2026, 9, 30))
                .build();
        ReflectionTestUtils.setField(notification, "notificationId", notificationId);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 9, 24, 10, 30));
        return notification;
    }
}
