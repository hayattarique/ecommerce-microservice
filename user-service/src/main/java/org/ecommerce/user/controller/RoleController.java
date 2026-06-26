package org.ecommerce.user.controller;

import lombok.RequiredArgsConstructor;
import org.ecommerce.user.dto.RoleDto;
import org.ecommerce.user.service.RoleService;
import org.ecommerce.utility.constants.ApiMessages;
import org.ecommerce.utility.constants.RoleMappingConstant;
import org.ecommerce.utility.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoleMappingConstant.BASE_URL)
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping(RoleMappingConstant.ADD_ROLE)
    public ResponseEntity<ApiResponse<Object>> addRole(@RequestBody RoleDto role) {
        return ResponseEntity.ok(ApiResponse.success(roleService.addRole(role), ApiMessages.OPERATION_SUCCESSFUL));
    }
}
