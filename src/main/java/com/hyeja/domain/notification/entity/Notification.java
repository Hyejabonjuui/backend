package com.hyeja.domain.notification.entity;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.global.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_policy"))
    private Policy policy;

    @Column(name = "read_yn", nullable = false)
    private Boolean readYn;

    @Builder
    public Notification(Member member, Policy policy) {
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.policy = Objects.requireNonNull(policy, "정책은 필수입니다.");
        this.readYn = false;
    }

    public void markAsRead() {
        this.readYn = true;
    }
}
