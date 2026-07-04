package org.ecommerce.user.mapper;

import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mappings({
            @Mapping(target = "roles", ignore = true),
            @Mapping(target = "permissions", ignore = true)
    })
    UserEntity toEntity(UserDto userDto);

    @Mappings({
            @Mapping(target = "roles", ignore = true),
            @Mapping(target = "permissions", ignore = true)
    })
    UserDto toDto(UserEntity userEntity);
}
