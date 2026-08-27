package org.ecommerce.auth.service.dto;

import jakarta.validation.constraints.NotEmpty;

public record LogoutRequest(@NotEmpty String accessToken, @NotEmpty String refreshToken) {
}
