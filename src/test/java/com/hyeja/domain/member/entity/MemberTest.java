package com.hyeja.domain.member.entity;

import com.hyeja.domain.member.enums.Role;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class MemberTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsMemberWithGeneratedIdAndUserRole() {
        Member member = newMember("member@example.com");
        entityManager.persist(member);
        entityManager.flush();
        entityManager.clear();

        Member stored = entityManager.find(Member.class, member.getMemberId());
        assertThat(stored.getEmail()).isEqualTo("member@example.com");
        assertThat(stored.getPassword()).isEqualTo("encoded-password");
        assertThat(stored.getNickname()).isEqualTo("혜자");
        assertThat(stored.getRole()).isEqualTo(Role.USER);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    void rejectsDuplicateEmail() {
        entityManager.persist(newMember("duplicate@example.com"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.persist(newMember("duplicate@example.com"));
            entityManager.flush();
        }).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    private Member newMember(String email) {
        return Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname("혜자")
                .build();
    }
}
