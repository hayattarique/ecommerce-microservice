package org.ecommerce.user.service;

import org.ecommerce.user.dto.RoleDto;

public interface RoleService {
    /**
     * this method is used add (persist role)
     * @link RoleService
     * @param role dto
     * @return persisted role
     */
    RoleDto addRole(RoleDto role);
}
