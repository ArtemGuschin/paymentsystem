package com.artem.webhookcollectorservice.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationEventProducer {

    private final DefaultKafkaProducerFactory<String, NotificationCreated>
            producerFactory;

    private final KafkaTemplate<String, NotificationCreated> kafkaTemplate;

    public NotificationEventProducer(
            @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}")
            String bootstrapServers,

            @Value("${SCHEMA_REGISTRY_URL:http://localhost:8086}")
            String schemaRegistryUrl
    ) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class
        );

        properties.put(
                "schema.registry.url",
                schemaRegistryUrl
        );

        this.producerFactory =
                new DefaultKafkaProducerFactory<>(properties);

        this.kafkaTemplate =
                new KafkaTemplate<>(producerFactory);
    }

    public void send(
            String topic,
            String key,
            NotificationCreated event
    ) throws Exception {

        kafkaTemplate
                .send(
                        topic,
                        key,
                        event
                )
                .get();
    }

    @PreDestroy
    public void close() {
        producerFactory.destroy();
    }
}