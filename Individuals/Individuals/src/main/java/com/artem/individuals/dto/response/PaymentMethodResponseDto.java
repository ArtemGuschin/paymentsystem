package com.artem.individuals.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponseDto {

    private Long id;

    private String name;

    private String providerMethodType;

    private String imageUrl;

    private List<RequiredFieldDto> requiredFields;

}