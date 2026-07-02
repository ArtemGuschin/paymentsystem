package com.artem.individuals.mapper;


import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.response.PaymentMethodResponseDto;
import com.artem.individuals.dto.response.PaymentResponseDto;
import com.artem.individuals.dto.response.RequiredFieldDto;
import com.artem.paymentservice.dto.PaymentMethodResponse;
import com.artem.paymentservice.dto.PaymentRequest;
import com.artem.paymentservice.dto.PaymentResponse;
import com.artem.paymentservice.dto.RequiredField;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMapper {

    /**
     * Individuals -> PaymentService
     */
    public PaymentRequest toPaymentRequest(PaymentRequestDto dto) {

        return new PaymentRequest()
                .internalTransactionUid(dto.getInternalTransactionUid())
                .methodId(dto.getMethodId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .userFields(dto.getUserFields());
    }

    /**
     * PaymentService -> Individuals
     */
    public PaymentResponseDto toPaymentResponseDto(PaymentResponse response) {

        return PaymentResponseDto.builder()
                .providerTransactionId(response.getProviderTransactionId())
                .build();
    }

    /**
     * PaymentService -> Individuals
     */
    public PaymentMethodResponseDto toPaymentMethodResponseDto(PaymentMethodResponse response) {

        return PaymentMethodResponseDto.builder()
                .id(response.getId())
                .name(response.getName())
                .providerMethodType(response.getProviderMethodType())
                .imageUrl(response.getImageUrl())
                .requiredFields(
                        response.getRequiredFields()
                                .stream()
                                .map(this::toRequiredFieldDto)
                                .toList()
                )
                .build();
    }

    /**
     * PaymentService -> Individuals
     */
    public RequiredFieldDto toRequiredFieldDto(RequiredField field) {

        return RequiredFieldDto.builder()
                .uid(field.getUid())
                .name(field.getName())
                .description(field.getDescription())
                .placeholder(field.getPlaceholder())
                .dataType(field.getDataType())
                .validationType(field.getValidationType())
                .validationRule(field.getValidationRule())
                .defaultValue(field.getDefaultValue())
                .valuesOptions(field.getValuesOptions())
                .build();
    }

    /**
     * PaymentService -> Individuals
     */
    public List<PaymentMethodResponseDto> toPaymentMethodResponseDtoList(
            List<PaymentMethodResponse> responses) {

        return responses.stream()
                .map(this::toPaymentMethodResponseDto)
                .toList();
    }

}
