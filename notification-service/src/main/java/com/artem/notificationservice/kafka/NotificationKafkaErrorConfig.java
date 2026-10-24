
package com.artem.notificationservice.kafka;

import com.artem.common.notification.avro.NotificationCreated;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class NotificationKafkaErrorConfig {



    @Bean
    public ProducerFactory<String, Object> notificationDltProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(
                kafkaProperties.buildProducerProperties(null)
        );

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        Map<Class<?>, Serializer<?>> serializers =
                new LinkedHashMap<>();

        serializers.put(
                byte[].class,
                new ByteArraySerializer()
        );

        serializers.put(
                NotificationCreated.class,
                new KafkaAvroSerializer()
        );

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                new DelegatingByTypeSerializer(serializers)
        );
    }


    @Bean
    public KafkaTemplate<String, Object> notificationDltKafkaTemplate(
            @Qualifier("notificationDltProducerFactory")
            ProducerFactory<String, Object> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DefaultErrorHandler notificationKafkaErrorHandler(
            @Qualifier("notificationDltKafkaTemplate")
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        -1
                                )
                );

        // Не считаем сообщение обработанным,
        // пока Kafka не подтвердит публикацию в DLT.
        recoverer.setFailIfSendResultIsError(true);

        // 3 повторные попытки с интервалом 1 секунда
        // для ошибок, допускающих повторную обработку.
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L)
        );
    }

    @Bean
    public NewTopic notificationCreatedDltTopic(
            @Value("${app.kafka.topics.notification-created}")
            String sourceTopic
    ) {
        return TopicBuilder
                .name(sourceTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
