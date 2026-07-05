package org.ecommerce.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.entity.RoleEntity;
import org.ecommerce.user.entity.UserEntity;
import org.ecommerce.user.entity.UserRoleEntity;
import org.ecommerce.user.mapper.RoleMapper;
import org.ecommerce.user.mapper.UserMapper;
import org.ecommerce.user.repositories.PermissionRepository;
import org.ecommerce.user.repositories.RoleRepository;
import org.ecommerce.user.repositories.UserRepository;
import org.ecommerce.user.repositories.UserRoleRepository;
import org.ecommerce.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;


    @Override
    @Transactional
    public UserDto registerUser(UserDto userDto) {
        UserEntity entity = userMapper.toEntity(userDto);
        entity.setStatus(true);
        userRepository.save(entity);
        userDto.setUserAccountId(entity.getId());
        return userDto;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow();
        List<RoleEntity> listOfRoles = userRoleRepository.findAllRolesByUserId(userEntity.getId());
        Set<String> roles = listOfRoles.stream().map(RoleEntity::getName).collect(Collectors.toSet());
        Set<String> permissions = new HashSet<>(permissionRepository.findAllPermissionsByEmail(email));
        UserDto dto = userMapper.toDto(userEntity);
        dto.setRoles(roles);
        dto.setPermissions(permissions);
        dto.setUserAccountId(userEntity.getId());
        return dto;
    }

    @Override
    @Transactional
    public String assignRoleToUser(Long id, RoleDto roleDto) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow();
        RoleEntity roleEntity = roleRepository.findById(roleDto.getId()).orElseThrow();

        if (userRoleRepository.existByUserIdAndRoleId(userEntity.getId(), roleEntity.getId())) {
            throw new RuntimeException("Role " + roleEntity.getName() + " is already assigned to user " + userEntity.getEmail());
        }
        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setRole(roleEntity);
        userRoleEntity.setUser(userEntity);
        userRoleRepository.save(userRoleEntity);
        return "Role " + roleEntity.getName() + " assigned to user " + userEntity.getEmail();
    }
}
