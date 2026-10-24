
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

        NotificationOutboxEntity event =
                new NotificationOutboxEntity();

        event.setEventId(UUID.randomUUID().toString());
        event.setUserUid(userUid);
        event.setEmail(email);

        event.setNotificationType(USER_REGISTERED);
        event.setMessage("Welcome to Payment System");
        event.setSubject("Welcome to Payment System");
        event.setCreatedBy(SYSTEM);

        event.setEventCreatedAt(now);

        event.setStatus(NotificationOutboxEntity.Status.NEW);
        event.setAttemptCount(0);
        event.setNextAttemptAt(now);

        repository.saveAndFlush(event);

        log.info(
                "USER_REGISTERED notification saved to Outbox: eventId={}, userUid={}",
                event.getEventId(),
                userUid
        );

        return event.getEventId();
    }
}
