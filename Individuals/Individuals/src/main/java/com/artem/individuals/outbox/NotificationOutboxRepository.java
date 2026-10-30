
package com.artem.individuals.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutboxEntity, UUID> {

    List<NotificationOutboxEntity>
    findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationOutboxEntity.Status status,
            Instant now,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO notification_outbox (
            id,
            event_id,
            user_uid,
            email,
            notification_type,
            message,
            subject,
            created_by,
            event_created_at,
            status,
            attempt_count,
            next_attempt_at,
            created_at
        )
        VALUES (
            :id,
            :eventId,
            :userUid,
            :email,
            'USER_REGISTERED',
            'Welcome to Payment System',
            'Welcome to Payment System',
            'SYSTEM',
            :now,
            'NEW',
            0,
            :now,
            :now
        )
            ON CONFLICT (user_uid)
            WHERE notification_type = 'USER_REGISTERED'
          AND registration_canonical = TRUE
            DO NOTHING
        """, nativeQuery = true)
    int insertUserRegisteredIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") String eventId,
            @Param("userUid") UUID userUid,
            @Param("email") String email,
            @Param("now") Instant now
    );

    @Query(value = """
        SELECT *
        FROM notification_outbox
        WHERE user_uid = :userUid
          AND notification_type = :notificationType
          AND registration_canonical = TRUE
        """, nativeQuery = true)
    Optional<NotificationOutboxEntity> findByUserUidAndNotificationType(
            @Param("userUid") UUID userUid,
            @Param("notificationType") String notificationType
    );
}
