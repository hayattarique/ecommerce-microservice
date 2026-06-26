package org.ecommerce.user.mapper;

import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleEntity toEntity(RoleDto roleDto);

    RoleDto toDto(RoleEntity roleEntity);
}
