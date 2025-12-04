package com.prpo.chat.service;

import com.prpo.chat.entities.User;
import com.prpo.chat.service.dto.CreateUserRequestDto;
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

  public User createUser(final CreateUserRequestDto request) {
    if(userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already in use.");
    }
    if(userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Username already in use");
    }

    // main info
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    // TODO: call authentication service
    user.setPasswordHash(request.getPassword());


    // default settings
    final User.Settings settings = new User.Settings();
    settings.setTheme(User.Theme.DARK);
    settings.setNotifications(true);

    //friends
    user.setFriends(List.of());

    return userRepository.save(user);
  }
}
