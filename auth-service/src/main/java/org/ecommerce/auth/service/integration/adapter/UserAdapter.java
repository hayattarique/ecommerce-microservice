package org.ecommerce.auth.service.integration.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.exception.ServiceException;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.mapper.AuthenticationMapper;
import org.ecommerce.utility.constants.ApiMessages;
import org.ecommerce.utility.util.ApiResponse;
import org.springframework.beans.BeanUtils;
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
            throw new ServiceException("USER-CLIENT RESPONSE NOT SUCCESSFUL " + email,ApiMessages.INTERNAL_ERROR,500);
        } catch (Exception e) {
            log.error("ERROR CALLING USER-CLIENT {}", email, e);
            throw new IllegalStateException("ERROR CALLING USER-CLIENT " + email, e);
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
                throw new ServiceException("USER-CLIENT REGISTER RESPONSE NOT SUCCESSFUL " + signupRequest.getEmail(),
                        ApiMessages.REGISTRATION_FAILED,response.getStatusCode().value());
            }
        } catch (ServiceException e) {
            log.error("ERROR CALLING USER-CLIENT REGISTER {}", signupRequest.getEmail(), e);
            throw new ServiceException("ERROR CALLING USER-CLIENT REGISTER " + signupRequest.getEmail(), ApiMessages.REGISTRATION_FAILED, e.getStatusCode());
        }

    }

}
