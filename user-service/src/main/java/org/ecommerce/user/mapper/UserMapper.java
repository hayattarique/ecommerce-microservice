package org.ecommerce.user.mapper;

import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(UserDto userDto);

    UserDto toDto(UserEntity userEntity);
}
