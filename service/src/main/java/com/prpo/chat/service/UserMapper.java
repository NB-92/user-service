package com.prpo.chat.service;

import com.prpo.chat.entities.User;
import com.prpo.chat.service.dtos.RegisterRequestDto;
import com.prpo.chat.service.dtos.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity (RegisterRequestDto request);

    UserDto toUserDto(User user);
}
