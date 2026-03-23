package com.artem.currencyrateservice.mapper;


import com.artem.currencyrateservice.dto.RateResponse;
import com.artem.currencyrateservice.entity.ConversionRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface RateMapper {

    @Mapping(source = "sourceCurrency.code", target = "sourceCode")
    @Mapping(source = "destinationCurrency.code", target = "destinationCode")
    @Mapping(source = "rateBeginTime", target = "rateTimestamp")
    @Mapping(source = "provider.providerCode", target = "providerCode")
    RateResponse toDto(ConversionRate rate);


    default OffsetDateTime map(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

}
