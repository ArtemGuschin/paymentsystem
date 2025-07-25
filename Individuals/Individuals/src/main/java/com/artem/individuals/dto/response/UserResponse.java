package com.artem.individuals.dto.response;


import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserResponse {
    private String id;
    private String email;
    public List<String> roles;
    private String createdAt;
}