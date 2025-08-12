package com.artem.personservice.dto;




import lombok.Data;

@Data
public class UserUpdateRequest {
    private String email;
    private String firstName;
    private String lastName;
    private AddressDto address;
    private IndividualDto individual;
}