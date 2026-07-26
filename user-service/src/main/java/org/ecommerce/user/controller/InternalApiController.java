package org.ecommerce.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.user.service.UserService;
import org.ecommerce.utility.commons.constants.InternalMappingConstant;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(InternalMappingConstant.BASE_URL)
@RequiredArgsConstructor
@RestController
@Slf4j
public class InternalApiController {

    private final UserService userService;

    @GetMapping(InternalMappingConstant.GET_USER_BY_EMAIL)
    public ResponseEntity<ApiResponse<Object>> login(@PathVariable String email) {
        log.info("Received login request for user: {}", email);
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email), "User retrieved successfully"));
    }

}


