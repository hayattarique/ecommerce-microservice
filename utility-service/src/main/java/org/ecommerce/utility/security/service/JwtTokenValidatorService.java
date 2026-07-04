package org.ecommerce.utility.security.service;

import io.jsonwebtoken.Claims;

public interface JwtTokenValidatorService {


 Claims validateTokenAndGetClaims(String token);
}
