
package com.artem.individuals.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private static final String USER_REGISTERED = "USER_REGISTERED";
    private static final String SYSTEM = "SYSTEM";

    private final NotificationOutboxRepository repository;

    @Transactional
    public String saveUserRegistered(
            UUID userUid,
            String email
    ) {
        Instant now = Instant.now();

        // 1. Пытаемся создать USER_REGISTERED.
        // Если событие уже есть, PostgreSQL пропустит INSERT.
        int inserted = repository.insertUserRegisteredIfAbsent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                userUid,
                email,
                now
        );

        // 2. Получаем событие из PostgreSQL.
        // Это может быть как новая, так и существующая запись.
        NotificationOutboxEntity event = repository
                .findByUserUidAndNotificationType(
                        userUid,
                        USER_REGISTERED
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "USER_REGISTERED event not found for userUid="
                                        + userUid
                        )
                );

        // 3. Возвращаем первоначальный eventId.
        log.info(
                "USER_REGISTERED Outbox: eventId={}, userUid={}, inserted={}",
                event.getEventId(),
                userUid,
                inserted == 1
        );

        return event.getEventId();
    }
}
