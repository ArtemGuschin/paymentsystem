package com.artem.fakepaymentprovider.mapper;


import com.artem.fakepaymentprovider.dto.Transaction;
import com.artem.fakepaymentprovider.dto.TransactionRequest;
import com.artem.fakepaymentprovider.model.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    @Mapping(target = "createdAt", expression = "java(map(entity.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(map(entity.getUpdatedAt()))")
    Transaction toDto(TransactionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "merchant", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TransactionEntity toEntity(TransactionRequest request);

    // 🔥 ВОТ ЭТО КЛЮЧ
    default java.time.OffsetDateTime map(java.time.Instant instant) {
        return instant == null ? null :
                instant.atOffset(java.time.ZoneOffset.UTC);
    }
}