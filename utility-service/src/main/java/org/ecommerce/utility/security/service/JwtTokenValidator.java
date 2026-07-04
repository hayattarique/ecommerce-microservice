package org.ecommerce.utility.security.service;

import io.jsonwebtoken.Claims;

public interface JwtTokenValidator {


 Claims validateTokenAndGetClaims(String token);
}
