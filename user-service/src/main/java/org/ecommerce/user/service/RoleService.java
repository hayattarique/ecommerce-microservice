package org.ecommerce.user.service;

import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.utility.util.PageResponse;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    /**
     * this method is used add (persist role)
     * @link RoleService
     * @param role dto
     * @return persisted role
     */
    RoleDto addRole(RoleDto role);


    /**
     * this method is used to get all roles paginated
     * @return page
     */

    PageResponse<RoleDto> getRoles(Pageable pageable);
}
