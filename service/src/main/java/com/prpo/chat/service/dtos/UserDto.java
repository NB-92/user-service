package com.prpo.chat.service.dtos;

import com.prpo.chat.entities.User;
import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String username;

    private User.Profile profile;
}
