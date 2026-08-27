package org.ecommerce.auth.service.integration.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.integration.client.InternalClient;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.mapper.AuthenticationMapper;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserAdapter {

    private final UserClient userClient;
    private final InternalClient internalClient;
    private final AuthenticationMapper mapper;
    private final JWTPropertiesConfig jwtPropertiesConfig;

    public UserDto getUserByEmail(String email) {
        log.info("CALLING USER-CLIENT GET-USER-BY-EMAIL {}", email);

        return required(internalClient.findUserByEmail(email, jwtPropertiesConfig.getApiKey()),
                email, AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
    }

    public UserDto register(SignupRequest signupRequest) {
        log.info("CALLING USER-CLIENT REGISTER {}", signupRequest.getEmail());

        return required(userClient.register(mapper.signupRequestToUserDto(signupRequest)),
                signupRequest.getEmail(), AuthErrorCode.REGISTRATION_FAILED);
    }

    private UserDto required(ResponseEntity<ApiResponse<UserDto>> response, String email, ErrorCode errorCode) {
        ApiResponse<UserDto> body = response.getBody();
        UserDto data = body == null ? null : body.getData();

        if (data == null) {
            log.warn("USER-CLIENT RETURNED AN EMPTY BODY FOR {}", email);
            throw new DownstreamServiceException(errorCode);
        }
        log.info("USER-CLIENT RESPONSE {}", data);
        return data;
    }
}
