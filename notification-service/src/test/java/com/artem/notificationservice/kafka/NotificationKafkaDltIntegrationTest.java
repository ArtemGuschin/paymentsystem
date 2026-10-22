
package com.artem.notificationservice.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import com.artem.notificationservice.service.NotificationService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.time.Instant;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = NotificationKafkaDltIntegrationTest.KafkaTestApplication.class,
        properties = {
                "spring.kafka.consumer.group-id=notification-dlt-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
                "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=io.confluent.kafka.serializers.KafkaAvroDeserializer",
                "spring.kafka.consumer.properties.specific.avro.reader=true",
                "spring.kafka.properties.schema.registry.url=mock://notification-dlt-test",
                "app.kafka.topics.notification-created=notification.created"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "notification.created",
                "notification.created.DLT"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class NotificationKafkaDltIntegrationTest {

    @SpringBootConfiguration
    @EnableKafka
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({
            NotificationCreatedConsumer.class,
            NotificationKafkaErrorConfig.class
    })
    static class KafkaTestApplication {
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, Object> notificationDltKafkaTemplate;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void invalidAvroShouldGoToDltAndValidEventShouldBeProcessed()
            throws Exception {

        byte[] invalidAvro = {1, 2, 3, 4, 5};

        String eventId = UUID.randomUUID().toString();

        NotificationCreated validEvent =
                NotificationCreated.newBuilder()
                        .setEventId(eventId)
                        .setUserUid(UUID.randomUUID().toString())
                        .setNotificationType("USER_REGISTERED")
                        .setMessage("Welcome to Payment System")
                        .setSubject("Welcome")
                        .setCreatedBy("SYSTEM")
                        .setEmail("user@example.com")
                        .setCreatedAt(Instant.now())

                        .build();

        // Сначала отправляем повреждённое сообщение.
        notificationDltKafkaTemplate.send(
                "notification.created",
                "invalid-event",
                invalidAvro
        ).get(10, TimeUnit.SECONDS);

        // Затем корректное Avro-событие.
        notificationDltKafkaTemplate.send(
                "notification.created",
                "valid-event",
                validEvent
        ).get(10, TimeUnit.SECONDS);

        // Корректное событие должно дойти до Consumer.
        verify(notificationService, timeout(20_000).times(1))
                .process(argThat(event ->
                        eventId.equals(event.getEventId())
                ));

        // Читаем DLT как массив байтов.
        var consumerFactory =
                new DefaultKafkaConsumerFactory<String, byte[]>(
                        KafkaTestUtils.consumerProps(
                                "dlt-verifier-" + UUID.randomUUID(),
                                "false",
                                embeddedKafka
                        ),
                        new StringDeserializer(),
                        new ByteArrayDeserializer()
                );

        try (var consumer = consumerFactory.createConsumer()) {

            consumer.subscribe(
                    List.of("notification.created.DLT")
            );

            var dltRecord = KafkaTestUtils.getSingleRecord(
                    consumer,
                    "notification.created.DLT",
                    Duration.ofSeconds(20)
            );

            // Исходные повреждённые байты должны сохраниться.
            assertArrayEquals(
                    invalidAvro,
                    dltRecord.value()
            );
        }

        // Только корректное событие передано бизнес-сервису.
        verify(notificationService, times(1))
                .process(any(NotificationCreated.class));
    }
}
