package org.ecommerce.auth.service.integration.client;

import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.utility.commons.constants.UserMappingConstant;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange( UserMappingConstant.BASE)
public interface UserClient {

    @PostExchange(UserMappingConstant.REGISTER)
    ResponseEntity<ApiResponse<UserDto>> register(@RequestBody UserDto user);

    @GetExchange(UserMappingConstant.GET_PERMISSIONS_BY_USERNAME_OR_EMAIL)
    ResponseEntity<ApiResponse<List<String>>> findPermissionsByUsername(@PathVariable String email);

    @GetExchange(UserMappingConstant.GET_USER_BY_EMAIL)
    ResponseEntity<ApiResponse<UserDto>> findUserByEmail(@PathVariable String email);

}
