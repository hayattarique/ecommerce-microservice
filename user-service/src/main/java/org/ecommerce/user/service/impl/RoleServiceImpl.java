package org.ecommerce.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.entity.RoleEntity;
import org.ecommerce.user.mapper.RoleMapper;
import org.ecommerce.user.repositories.RoleRepository;
import org.ecommerce.user.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleDto addRole(RoleDto role) {
        log.info("ADDING ROLE {} TO ID {}", role.getName(), role.getId());
        RoleEntity entity = roleMapper.toEntity(role);
        roleRepository.save(entity);

        return roleMapper.toDto(entity);
    }
}
