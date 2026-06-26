package org.ecommerce.auth.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticationRequest {
    @Email(message = "Invalid email format")
    private String email;
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
