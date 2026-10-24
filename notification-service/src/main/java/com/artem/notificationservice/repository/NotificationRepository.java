
package com.artem.notificationservice.repository;

import com.artem.notificationservice.model.EmailDeliveryStatus;
import com.artem.notificationservice.model.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findAllByUserUidOrderByCreatedAtDesc(
            String userUid
    );

    Optional<NotificationEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Query("""
            SELECT n.id
            FROM NotificationEntity n
            WHERE n.emailDeliveryStatus IN :statuses
              AND n.emailNextAttemptAt <= :now
            ORDER BY n.emailNextAttemptAt ASC, n.id ASC
            """)
    List<Long> findDueDeliveryIds(
            @Param("statuses") Collection<EmailDeliveryStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE NotificationEntity n
            SET n.emailDeliveryStatus = :sending,
                n.emailNextAttemptAt = :leaseUntil
            WHERE n.id = :id
              AND n.emailDeliveryStatus IN :statuses
              AND n.emailNextAttemptAt <= :now
            """)
    int claimEmailDelivery(
            @Param("id") Long id,
            @Param("statuses") Collection<EmailDeliveryStatus> statuses,
            @Param("sending") EmailDeliveryStatus sending,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );
}
