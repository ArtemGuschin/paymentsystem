package com.artem.notificationservice.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(NotificationCreated event) {

        log.info(
                "Received notification.created event: eventId={}, userUid={}, type={}",
                event.getEventId(),
                event.getUserUid(),
                event.getNotificationType()
        );

        notificationService.process(event);
    }
}