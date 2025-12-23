package com.prpo.chat.service;

import com.prpo.chat.entities.User;
import com.prpo.chat.service.dto.CreateUserRequestDto;
import com.prpo.chat.service.dtos.RegisterRequestDto;
import com.prpo.chat.service.dtos.UserDto;
import com.prpo.chat.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
  @Autowired
  private final UserRepository userRepository;

  // TODO: use mappers
  public UserDto registerUser(final RegisterRequestDto request) {
    if(userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already in use.");
    }
    if(userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Username already in use");
    }

    // main info
    var user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    // TODO: call authentication service
    user.setPasswordHash(request.getPassword());

    // profile
    final var profile = new User.Profile();
    user.setProfile(profile);
    profile.setBio("");
    profile.setBirthdate(null);
    profile.setAvatarUrl("https://example.com/avatar.jpg");

    // default settings
    final var settings = new User.Settings();
    user.setSettings(settings);
    settings.setTheme(User.Theme.DARK);
    settings.setNotifications(true);

    //friends
    user.setFriends(List.of());

    user = userRepository.save(user);
    final var userDto = new UserDto();
    userDto.setId(user.getId());
    userDto.setUsername(user.getUsername());
    userDto.setProfile(user.getProfile());
    return userDto;
  }
}
