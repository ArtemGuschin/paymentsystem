
package com.artem.individuals.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
