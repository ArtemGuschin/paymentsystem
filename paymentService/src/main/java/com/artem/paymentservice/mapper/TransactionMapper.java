package com.artem.paymentservice.mapper;


import com.artem.fakepaymentprovider.client.dto.TransactionRequest;
import com.artem.paymentservice.dto.PaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "method", source = "method")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "notificationUrl", ignore = true)
    TransactionRequest toTransactionRequest(
            PaymentRequest request,
            String method
    );

}