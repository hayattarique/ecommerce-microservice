package org.ecommerce.auth.service.integration.client;

import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.utility.commons.constants.InternalMappingConstant;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(InternalMappingConstant.BASE_URL)
public interface InternalClient {

    @GetExchange(InternalMappingConstant.GET_USER_BY_EMAIL)
    ResponseEntity<ApiResponse<UserDto>> findUserByEmail(@PathVariable String email, @RequestHeader(SecurityConstants.X_INTERNAL_API_KEY) String apiKey);
}
