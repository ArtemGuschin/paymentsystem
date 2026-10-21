
package com.artem.webhookcollectorservice.repository;

import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository
        extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity>
    findTop100ByStatusAndEventTypeAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status,
            String eventType,
            LocalDateTime now
    );
}
