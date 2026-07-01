package org.ecommerce.user.mapper;

import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", ignore = true)
    UserEntity toEntity(UserDto userDto);

    @Mapping(target = "roles", ignore = true)
    UserDto toDto(UserEntity userEntity);
}
