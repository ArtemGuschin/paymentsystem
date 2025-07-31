package com.artem.personservice.dto;



import lombok.Data;

@Data
public class UserCreateRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private AddressDto address;
    private IndividualDto individual;
}
