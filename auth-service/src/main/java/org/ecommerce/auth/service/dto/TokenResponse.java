package org.ecommerce.auth.service.dto;

/**
 * What login and refresh return in the body. The refresh token is NOT here:
 * it travels only in the HttpOnly cookie, so JavaScript can never read it.
 */
public record TokenResponse(Long id, String token) {
}
