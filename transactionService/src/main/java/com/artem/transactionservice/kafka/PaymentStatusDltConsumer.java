package com.artem.transactionservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusDltConsumer {

    private static final String MAIN_TOPIC =
            "payment.status.updated";

    private static final String DLT_TOPIC =
            "payment.status.updated.DLT";

    /*
     * Этим header помечаем сообщение,
     * которое уже один раз вернули из DLT
     * обратно в основной topic.
     */
    private static final String REPROCESSED_HEADER =
            "payment-status-reprocessed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = DLT_TOPIC,
            groupId = "transaction-service-payment-status-dlt-reprocessor"
    )
    public void handle(
            ConsumerRecord<String, String> record
    ) {

        /*
         * Если header уже существует,
         * значит сообщение уже проходило:
         *
         * main
         * -> retries
         * -> DLT
         * -> main
         * -> retries
         * -> DLT
         *
         * Второй раз его НЕ возвращаем.
         */
        if (record.headers()
                .lastHeader(REPROCESSED_HEADER) != null) {

            log.error(
                    "Payment status event failed after DLT reprocessing. " +
                            "Manual intervention required. key={}, partition={}, offset={}",
                    record.key(),
                    record.partition(),
                    record.offset()
            );

            return;
        }

        log.warn(
                "Reprocessing payment status event from DLT. key={}, partition={}, offset={}",
                record.key(),
                record.partition(),
                record.offset()
        );

        ProducerRecord<String, Object> retryRecord =
                new ProducerRecord<>(
                        MAIN_TOPIC,
                        record.key(),
                        record.value()
                );

        /*
         * Ставим защитную метку.
         *
         * Если сообщение снова упадёт
         * и вернётся в DLT,
         * второй раз его уже не отправим.
         */
        retryRecord.headers().add(
                REPROCESSED_HEADER,
                new byte[]{1}
        );

        try {

            kafkaTemplate
                    .send(retryRecord)
                    .get(10, TimeUnit.SECONDS);

            log.info(
                    "Payment status event successfully returned from DLT to main topic. key={}",
                    record.key()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "DLT reprocessing was interrupted",
                    ex
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to return payment status event from DLT to main topic",
                    ex
            );
        }
    }
}
