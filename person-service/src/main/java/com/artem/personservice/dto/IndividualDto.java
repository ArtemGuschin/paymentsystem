package com.artem.personservice.dto;



import lombok.Data;
import java.util.UUID;

@Data
public class IndividualDto {
    private UUID id;
    private String passportNumber;
    private String phoneNumber;
}