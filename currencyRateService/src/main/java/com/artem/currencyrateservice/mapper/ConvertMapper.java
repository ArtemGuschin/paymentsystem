package com.artem.currencyrateservice.mapper;



import com.artem.currencyrateservice.dto.ConvertRequest;
import com.artem.currencyrateservice.dto.ConvertResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConvertMapper {

    @Mapping(source = "request.sourceCode", target = "sourceCode")
    @Mapping(source = "request.destinationCode", target = "destinationCode")
    @Mapping(source = "request.amount", target = "sourceAmount")
    @Mapping(source = "convertedAmount", target = "convertedAmount")
    @Mapping(source = "rate", target = "rate")
    ConvertResponse toDto(ConvertRequest request, Double convertedAmount, Double rate);

}
