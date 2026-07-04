package org.ecommerce.utility.security.service;

import io.jsonwebtoken.Claims;
import org.ecommerce.utility.security.model.AuthenticatedUser;

public interface JwtClaimExtractorService {

    AuthenticatedUser extractUserDetailsFromToken(Claims claims);
}
