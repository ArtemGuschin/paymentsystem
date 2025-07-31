package com.artem.personservice.dto;



import lombok.Data;
import java.util.UUID;

@Data
public class AddressDto {
    private UUID id;
    private Integer countryId;
    private String address;
    private String zipCode;
    private String city;
    private String state;
}