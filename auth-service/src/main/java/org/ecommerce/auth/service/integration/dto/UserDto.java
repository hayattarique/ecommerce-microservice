package org.ecommerce.auth.service.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long userAccountId;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String gender;
    private String mobile;
    private Boolean active;
    private LocalDate dateOfBirth;
    private List<String> roles;
    private List<String> permissions;
}
