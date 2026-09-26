package com.hyeja.domain.notification.converter;

import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationItemDTO;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationListDTO;
import com.hyeja.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;

public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static NotificationItemDTO toNotificationItemDTO(Notification notification) {
        return NotificationItemDTO.builder()
                .notificationId(notification.getNotificationId())
                .memberId(notification.getMember().getMemberId())
                .policyId(notification.getPolicy().getPolicyId())
                .readYn(notification.getReadYn())
                .applyEndDate(notification.getDeadlineDate())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public static NotificationListDTO toNotificationListDTO(Page<Notification> notificationPage) {
        return NotificationListDTO.builder()
                .notifications(notificationPage.getContent().stream()
                        .map(NotificationConverter::toNotificationItemDTO)
                        .toList())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .build();
    }
}
