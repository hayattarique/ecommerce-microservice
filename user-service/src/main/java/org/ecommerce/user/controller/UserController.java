package org.ecommerce.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.service.UserService;
import org.ecommerce.utility.constants.UserMappingConstant;
import org.ecommerce.utility.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UserMappingConstant.BASE)
@Slf4j
@RequiredArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;

    @PostMapping(UserMappingConstant.REGISTER)
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody UserDto user) {
        log.info("Received registration request for user: {}", user.getEmail());
        UserDto userDto = userService.registerUser(user);
        return ResponseEntity.ok(ApiResponse.success(userDto, "User registered successfully"));

    }
    @GetMapping(UserMappingConstant.GET_USER_BY_EMAIL)
    public ResponseEntity<ApiResponse<Object>> login(@PathVariable String email) {
        log.info("Received login request for user: {}", email);
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email), "User retrieved successfully"));
    }


}
