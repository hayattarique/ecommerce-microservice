package org.ecommerce.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationDto {
    private Long id;
    private String token;
    private String refreshToken;
    /** Raw browser fingerprint: goes ONLY into the HttpOnly fingerprint cookie, never into a body. */
    private String fingerprint;
}
