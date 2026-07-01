package org.ecommerce.user.service;

import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.dto.UserDto;

public interface UserService {

    UserDto registerUser(UserDto userDto);

    UserDto getUserByEmail(String email);

    UserDto assignRoleToUser(Long id, RoleDto roleDto);
}
