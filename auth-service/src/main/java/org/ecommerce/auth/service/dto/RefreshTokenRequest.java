package org.ecommerce.auth.service.dto;

import jakarta.validation.constraints.NotEmpty;

public record RefreshTokenRequest(@NotEmpty String refreshToken){
}
