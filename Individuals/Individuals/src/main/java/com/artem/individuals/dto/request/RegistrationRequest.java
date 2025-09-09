package com.artem.individuals.dto.request;

import com.artem.model.AddressRequest;
import com.artem.model.IndividualRequest;
import com.artem.personservice.entity.AddressEntity;
import com.artem.personservice.entity.IndividualEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegistrationRequest extends AuthRequest {

    @JsonProperty("confirm_password")
    private String confirmPassword;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("individual")
    private IndividualRequest individual;

    @JsonProperty("address")
    private AddressRequest address;

    @JsonProperty("role")
    private String role;


}
