package com.hyeja.global.baseEntity;

import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class BaseEntityTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistAutomaticallyRecordsTimestamps() {
        AuditTestEntity entity = persistEntity();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void updatePreservesCreatedAtAndRefreshesUpdatedAt() {
        AuditTestEntity entity = persistEntity();
        LocalDateTime createdAt = entity.getCreatedAt();
        LocalDateTime updatedAt = entity.getUpdatedAt();
        entity.name = "updated";
        entityManager.flush();

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void softDeleteKeepsRowAndPreservesFirstDeletionTime() {
        AuditTestEntity entity = persistEntity();
        entity.softDelete();
        LocalDateTime deletedAt = entity.getDeletedAt();
        entity.softDelete();
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        entityManager.flush();
        entityManager.clear();

        AuditTestEntity stored = entityManager.find(AuditTestEntity.class, entity.id);
        assertThat(stored).isNotNull();
        assertThat(stored.isDeleted()).isTrue();
        assertThat(stored.getDeletedAt()).isNotNull();
    }

    private AuditTestEntity persistEntity() {
        AuditTestEntity entity = new AuditTestEntity();
        entity.name = "initial";
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    @Entity
    @Table(name = "base_entity_audit_test")
    static class AuditTestEntity extends BaseEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;

        protected AuditTestEntity() {
        }
    }
}
