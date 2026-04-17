package com.artem.fakepaymentprovider.mapper;


import com.artem.fakepaymentprovider.model.WebhookEntity;
import com.artem.fakepaymentprovider.dto.Webhook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebhookMapper {

    @Mapping(target = "receivedAt", expression = "java(map(entity.getReceivedAt()))")
    Webhook toDto(WebhookEntity entity);

    default java.time.OffsetDateTime map(java.time.Instant instant) {
        return instant == null ? null :
                instant.atOffset(java.time.ZoneOffset.UTC);
    }
}
