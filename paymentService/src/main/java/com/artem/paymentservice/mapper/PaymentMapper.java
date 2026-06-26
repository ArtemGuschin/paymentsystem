package com.artem.paymentservice.mapper;

import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.RequiredField;
import com.artem.paymentservice.model.PaymentMethod;
import com.artem.paymentservice.model.PaymentMethodRequiredField;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "imageUrl",
            source = "logo")
    @Mapping(target = "requiredFields",
            source = "requiredFields")
    PaymentMethodResponse toPaymentMethodResponse(
            PaymentMethod paymentMethod
    );

    RequiredField toRequiredField(
            PaymentMethodRequiredField field
    );

    default List<String> map(String value) {

        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .toList();
    }
}