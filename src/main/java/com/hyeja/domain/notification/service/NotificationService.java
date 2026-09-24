package com.hyeja.domain.notification.service;

import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.notification.converter.NotificationConverter;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationListDTO;
import com.hyeja.domain.notification.entity.Notification;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    public NotificationListDTO getNotifications(Long memberId, int page, int size) {
        if (!memberRepository.existsByMemberIdAndDeletedAtIsNull(memberId)) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Page<Notification> notificationPage = notificationRepository.findAllByMemberId(
                memberId,
                PageRequest.of(page, size)
        );
        return NotificationConverter.toNotificationListDTO(notificationPage);
    }
}
