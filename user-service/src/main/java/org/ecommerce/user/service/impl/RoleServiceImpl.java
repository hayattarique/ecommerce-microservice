package org.ecommerce.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.entity.RoleEntity;
import org.ecommerce.user.mapper.RoleMapper;
import org.ecommerce.user.repositories.RoleRepository;
import org.ecommerce.user.service.RoleService;
import org.ecommerce.utility.util.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        RoleEntity save = roleRepository.save(entity);

        return roleMapper.toDto(save);
    }

    @Override
    public PageResponse<RoleDto> getRoles(Pageable pageable) {
        Page<RoleEntity> roleEntityPage = roleRepository.findAll(pageable);
        List<RoleDto> roleList = roleEntityPage.getContent().stream().map(roleMapper::toDto).toList();
        return new PageResponse<>(roleList, roleEntityPage.getNumber(), roleEntityPage.getSize(), roleEntityPage.getTotalElements(), roleEntityPage.getTotalPages());
    }
}
