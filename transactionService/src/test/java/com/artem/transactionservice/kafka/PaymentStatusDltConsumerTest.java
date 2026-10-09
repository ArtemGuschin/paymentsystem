package com.artem.transactionservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentStatusDltConsumerTest {

    @Test
    void shouldNotReprocessMessageTwice() {

        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);

        PaymentStatusDltConsumer consumer =
                new PaymentStatusDltConsumer(
                        kafkaTemplate
                );

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "payment.status.updated.DLT",
                        0,
                        0L,
                        "test-key",
                        """
                        {
                          "transactionUid": "bad-id",
                          "status": "SUCCESS"
                        }
                        """
                );

        record.headers().add(
                "payment-status-reprocessed",
                new byte[]{1}
        );

        consumer.handle(record);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldReprocessDltMessageOnceAndAddProtectionHeader()
            throws Exception {

        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(future);

        PaymentStatusDltConsumer consumer =
                new PaymentStatusDltConsumer(
                        kafkaTemplate
                );

        String message =
                """
                {
                  "transactionUid": "bad-id",
                  "status": "SUCCESS"
                }
                """;

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "payment.status.updated.DLT",
                        0,
                        0L,
                        "test-key",
                        message
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, Object>> captor =
                ArgumentCaptor.forClass(
                        ProducerRecord.class
                );

        consumer.handle(record);

        verify(kafkaTemplate)
                .send(captor.capture());

        ProducerRecord<String, Object> sentRecord =
                captor.getValue();

        assertThat(sentRecord.topic())
                .isEqualTo("payment.status.updated");

        assertThat(sentRecord.key())
                .isEqualTo("test-key");

        assertThat(sentRecord.value())
                .isEqualTo(message);

        assertThat(
                sentRecord.headers()
                        .lastHeader(
                                "payment-status-reprocessed"
                        )
        ).isNotNull();
    }
}