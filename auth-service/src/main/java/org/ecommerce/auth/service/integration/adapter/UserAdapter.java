package org.ecommerce.auth.service.integration.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.mapper.AuthenticationMapper;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdapter {
    private final UserClient userClient;
    private final AuthenticationMapper mapper;

    public UserDto getUserByEmail(String email) {
        log.info("CALLING USER-CLIENT GET-USER-BY-EMAIL{}", email);
        try {
            ResponseEntity<ApiResponse<UserDto>> response = userClient.findUserByEmail(email);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("USER-CLIENT RESPONSE {}", response.getBody().getData());
                return response.getBody().getData();
            }
            log.warn("USER-CLIENT RESPONSE NOT SUCCESSFUL {}", email);
            throw new DownstreamServiceException(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
        } catch (Exception e) {
            log.error("ERROR CALLING USER-CLIENT {}", email, e);
            throw new DownstreamServiceException(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
        }

    }

    public UserDto register(SignupRequest signupRequest) {
        log.info("CALLING USER-CLIENT REGISTER {}", signupRequest.getEmail());

        try {
            UserDto userDto = mapper.signupRequestToUserDto(signupRequest);
            ResponseEntity<ApiResponse<UserDto>> response = userClient.register(userDto);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("USER-CLIENT REGISTER RESPONSE {}", response.getBody().getData());
                return response.getBody().getData();
            } else {
                log.warn("USER-CLIENT REGISTER RESPONSE NOT SUCCESSFUL {}", signupRequest.getEmail());
                throw new DownstreamServiceException(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
            }
        } catch (DownstreamServiceException e) {
            log.error("ERROR CALLING USER-CLIENT REGISTER {}", signupRequest.getEmail(), e);
            throw new DownstreamServiceException(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
        }

    }

}
