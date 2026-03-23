package com.artem.currencyrateservice.mapper;


import com.artem.currencyrateservice.dto.CurrencyResponse;
import com.artem.currencyrateservice.entity.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    CurrencyResponse toDto(Currency currency);

}
