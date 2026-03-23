package com.artem.currencyrateservice.mapper;



import com.artem.currencyrateservice.dto.RateProviderResponse;
import com.artem.currencyrateservice.entity.RateProvider;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RateProviderMapper {

    RateProviderResponse toDto(RateProvider provider);

}
