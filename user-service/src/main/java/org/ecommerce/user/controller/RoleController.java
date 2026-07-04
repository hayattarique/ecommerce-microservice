package org.ecommerce.user.controller;

import lombok.RequiredArgsConstructor;
import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.service.RoleService;
import org.ecommerce.utility.commons.constants.ApiMessages;
import org.ecommerce.utility.commons.constants.PageConstant;
import org.ecommerce.utility.commons.constants.RoleMappingConstant;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.ecommerce.utility.commons.util.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoleMappingConstant.BASE_URL)
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping(RoleMappingConstant.ADD_ROLE)
    public ResponseEntity<ApiResponse<Object>> addRole(@RequestBody RoleDto role) {
        return ResponseEntity.ok(ApiResponse.success(roleService.addRole(role), ApiMessages.OPERATION_SUCCESSFUL));
    }


    @GetMapping(RoleMappingConstant.GET_ALL_ROLES_BY_PAGINATED)
    public ResponseEntity<PageResponse<?>> getAllRolesPaginated(
            @RequestParam(defaultValue = PageConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = PageConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = PageConstant.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = PageConstant.DEFAULT_SORT_DIRECTION) String dir) {

        Pageable pageable =PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(dir), sortBy));
        Page<RoleDto> roles = roleService.getRoles(pageable);

        return ResponseEntity.ok(PageResponse.success(roles.getContent(), roles.getNumber(), roles.getSize(), roles.getTotalElements(), roles.getTotalPages()));
    }
}
