package com.artem.individuals.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

//@Data
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

    @JsonProperty("role")
    private String role;


}
