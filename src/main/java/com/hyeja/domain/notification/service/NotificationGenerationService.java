package com.hyeja.domain.notification.service;

import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.notification.repository.NotificationRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationGenerationService {

    private static final int DEADLINE_NOTICE_DAYS = 7;

    private final MemberService memberService;
    private final FavoriteRepository favoriteRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public int createDeadlineNotifications(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "알림 생성 기준일은 필수입니다.");
        LocalDate deadlineDate = baseDate.plusDays(DEADLINE_NOTICE_DAYS);
        List<Favorite> targets = favoriteRepository
                .findNotificationTargetsByDeadlineDate(deadlineDate);
        return createNotifications(
                targets,
                deadlineDate,
                () -> notificationRepository
                        .findAllByDeadlineDateWithMemberAndPolicy(deadlineDate)
        );
    }

    @Transactional
    public int createDeadlineNotificationsForMember(LocalDate baseDate, Long memberId) {
        Objects.requireNonNull(baseDate, "알림 생성 기준일은 필수입니다.");
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        memberService.getActiveMember(memberId);

        LocalDate deadlineDate = baseDate.plusDays(DEADLINE_NOTICE_DAYS);
        List<Favorite> targets = favoriteRepository
                .findNotificationTargetsByMemberIdAndDeadlineDate(memberId, deadlineDate);
        return createNotifications(
                targets,
                deadlineDate,
                () -> notificationRepository
                        .findAllByMemberIdAndDeadlineDateWithPolicy(memberId, deadlineDate)
        );
    }

    private int createNotifications(
            List<Favorite> targets,
            LocalDate deadlineDate,
            Supplier<List<Notification>> existingNotificationsSupplier
    ) {
        if (targets.isEmpty()) {
            return 0;
        }

        Set<NotificationKey> existingKeys = new HashSet<>();
        existingNotificationsSupplier.get()
                .forEach(notification -> existingKeys.add(NotificationKey.from(notification)));

        List<Notification> notifications = targets.stream()
                .filter(favorite -> existingKeys.add(NotificationKey.from(favorite, deadlineDate)))
                .map(favorite -> Notification.builder()
                        .member(favorite.getMember())
                        .policy(favorite.getPolicy())
                        .deadlineDate(deadlineDate)
                        .build())
                .toList();
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
        return notifications.size();
    }

    private record NotificationKey(Long memberId, String policyId, LocalDate deadlineDate) {

        private static NotificationKey from(Notification notification) {
            return new NotificationKey(
                    notification.getMember().getMemberId(),
                    notification.getPolicy().getPolicyId(),
                    notification.getDeadlineDate()
            );
        }

        private static NotificationKey from(Favorite favorite, LocalDate deadlineDate) {
            return new NotificationKey(
                    favorite.getMember().getMemberId(),
                    favorite.getPolicy().getPolicyId(),
                    deadlineDate
            );
        }
    }
}
