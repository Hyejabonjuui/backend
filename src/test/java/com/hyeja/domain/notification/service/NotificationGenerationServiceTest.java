package com.hyeja.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationGenerationServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 9, 26);
    private static final LocalDate DEADLINE_DATE = LocalDate.of(2026, 10, 3);

    @Mock
    private MemberService memberService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationGenerationService notificationGenerationService;

    @Test
    void createsUnreadNotificationsForDeadlineTargetsExceptExistingOnes() {
        Member firstMember = member(1L, "first@example.com");
        Member secondMember = member(2L, "second@example.com");
        Policy firstPolicy = policy("policy-1");
        Policy secondPolicy = policy("policy-2");
        List<Favorite> targets = List.of(
                favorite(firstMember, firstPolicy),
                favorite(secondMember, firstPolicy),
                favorite(firstMember, secondPolicy)
        );
        Notification existing = Notification.builder()
                .member(firstMember)
                .policy(firstPolicy)
                .deadlineDate(DEADLINE_DATE)
                .build();
        when(favoriteRepository.findNotificationTargetsByDeadlineDate(DEADLINE_DATE))
                .thenReturn(targets);
        when(notificationRepository.findAllByDeadlineDateWithMemberAndPolicy(DEADLINE_DATE))
                .thenReturn(List.of(existing));

        int result = notificationGenerationService.createDeadlineNotifications(BASE_DATE);

        assertThat(result).isEqualTo(2);
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(notification -> {
            assertThat(notification.getReadYn()).isFalse();
            assertThat(notification.getDeadlineDate()).isEqualTo(DEADLINE_DATE);
        });
        assertThat(captor.getValue())
                .extracting(
                        notification -> notification.getMember().getMemberId(),
                        notification -> notification.getPolicy().getPolicyId()
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(2L, "policy-1"),
                        org.assertj.core.groups.Tuple.tuple(1L, "policy-2")
                );
    }

    @Test
    void skipsAllNotificationsWhenSameMemberPolicyDeadlineAlreadyExists() {
        Member member = member(1L, "member@example.com");
        Policy policy = policy("policy-1");
        when(favoriteRepository.findNotificationTargetsByDeadlineDate(DEADLINE_DATE))
                .thenReturn(List.of(favorite(member, policy)));
        when(notificationRepository.findAllByDeadlineDateWithMemberAndPolicy(DEADLINE_DATE))
                .thenReturn(List.of(Notification.builder()
                        .member(member)
                        .policy(policy)
                        .deadlineDate(DEADLINE_DATE)
                        .build()));

        int result = notificationGenerationService.createDeadlineNotifications(BASE_DATE);

        assertThat(result).isZero();
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void returnsWithoutQueryingNotificationsWhenNoFavoriteTargetsExist() {
        when(favoriteRepository.findNotificationTargetsByDeadlineDate(DEADLINE_DATE))
                .thenReturn(List.of());

        int result = notificationGenerationService.createDeadlineNotifications(BASE_DATE);

        assertThat(result).isZero();
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void createsDeadlineNotificationsOnlyForRequestedMember() {
        Member requestedMember = member(1L, "requested@example.com");
        Policy firstPolicy = policy("policy-1");
        Policy secondPolicy = policy("policy-2");
        List<Favorite> targets = List.of(
                favorite(requestedMember, firstPolicy),
                favorite(requestedMember, secondPolicy)
        );
        Notification existing = Notification.builder()
                .member(requestedMember)
                .policy(firstPolicy)
                .deadlineDate(DEADLINE_DATE)
                .build();
        when(memberService.getActiveMember(1L)).thenReturn(requestedMember);
        when(favoriteRepository.findNotificationTargetsByMemberIdAndDeadlineDate(
                1L, DEADLINE_DATE)).thenReturn(targets);
        when(notificationRepository.findAllByMemberIdAndDeadlineDateWithPolicy(
                1L, DEADLINE_DATE)).thenReturn(List.of(existing));

        int result = notificationGenerationService
                .createDeadlineNotificationsForMember(BASE_DATE, 1L);

        assertThat(result).isEqualTo(1);
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(notification -> {
            assertThat(notification.getMember().getMemberId()).isEqualTo(1L);
            assertThat(notification.getPolicy().getPolicyId()).isEqualTo("policy-2");
            assertThat(notification.getDeadlineDate()).isEqualTo(DEADLINE_DATE);
            assertThat(notification.getReadYn()).isFalse();
        });
    }

    @Test
    void throwsMemberNotFoundWhenGeneratingForMissingMember() {
        when(memberService.getActiveMember(99L))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> notificationGenerationService
                .createDeadlineNotificationsForMember(BASE_DATE, 99L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);

        verifyNoInteractions(favoriteRepository, notificationRepository);
    }

    private Member member(Long memberId, String email) {
        Member member = Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname("회원")
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }

    private Policy policy(String policyId) {
        return Policy.builder()
                .policyId(policyId)
                .policyName("마감 예정 정책")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(false)
                .applyPeriodCode("PERIOD")
                .applyEndDate(DEADLINE_DATE)
                .build();
    }

    private Favorite favorite(Member member, Policy policy) {
        return Favorite.builder().member(member).policy(policy).build();
    }
}
