package com.artem.individuals.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredFieldDto {

    private UUID uid;

    private String name;

    private String description;

    private String placeholder;

    private String dataType;

    private String validationType;

    private String validationRule;

    private String defaultValue;

    private List<String> valuesOptions;

}