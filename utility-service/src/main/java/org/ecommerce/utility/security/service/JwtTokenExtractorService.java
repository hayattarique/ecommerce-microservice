package org.ecommerce.utility.security.service;

import io.jsonwebtoken.Claims;
import org.ecommerce.utility.security.model.AuthUserDetails;

public interface JwtTokenExtractorService {

    AuthUserDetails extractUserDetailsFromToken(Claims claims);
}
