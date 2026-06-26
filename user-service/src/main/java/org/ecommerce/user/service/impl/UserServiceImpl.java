package org.ecommerce.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.ecommerce.user.dto.UserDto;
import org.ecommerce.user.entity.UserEntity;
import org.ecommerce.user.mapper.UserMapper;
import org.ecommerce.user.repositories.UserRepository;
import org.ecommerce.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    @Transactional
    public UserDto registerUser(UserDto userDto) {
        UserEntity entity = userMapper.toEntity(userDto);
        entity.setActive(true);
        userRepository.save(entity);
        userDto.setUserAccountId(entity.getId());
        return userDto;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow();
        UserDto dto = userMapper.toDto(userEntity);
        dto.setUserAccountId(userEntity.getId());
        return dto;
    }
}
