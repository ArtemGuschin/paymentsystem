package com.artem.fakepaymentprovider.mapper;

import com.artem.fakepaymentprovider.model.PayoutEntity;
import com.artem.fakepaymentprovider.dto.Payout;
import com.artem.fakepaymentprovider.dto.PayoutRequest;

import org.mapstruct.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface PayoutMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    Payout toDto(PayoutEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "merchant", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PayoutEntity toEntity(PayoutRequest request);

    default OffsetDateTime map(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}