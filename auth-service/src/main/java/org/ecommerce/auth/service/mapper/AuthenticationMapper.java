package org.ecommerce.auth.service.mapper;

import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {

    @Mapping(target = "userAccountId", ignore = true)
    UserDto signupRequestToUserDto(SignupRequest signupRequest);
}
