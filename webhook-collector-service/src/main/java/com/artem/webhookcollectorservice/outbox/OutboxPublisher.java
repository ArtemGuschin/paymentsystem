package com.artem.webhookcollectorservice.outbox;

import com.artem.webhookcollectorservice.entity.OutboxEventEntity;
import com.artem.webhookcollectorservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String TOPIC = "payment.status.updated";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEventEntity> events =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.NEW
                );

        for (OutboxEventEntity event : events) {
            try {
                kafkaTemplate
                        .send(
                                TOPIC,
                                event.getAggregateId().toString(),
                                event.getPayload()
                        )
                        .get();

                event.markAsPublished();

                log.info(
                        "Outbox event published. eventId={}, aggregateId={}",
                        event.getUid(),
                        event.getAggregateId()
                );

            } catch (Exception e) {
                log.error(
                        "Failed to publish outbox event. eventId={}",
                        event.getUid(),
                        e
                );
            }
        }
    }
}