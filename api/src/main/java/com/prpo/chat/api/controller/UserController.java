package com.prpo.chat.api.controller;

import com.prpo.chat.service.dto.CreateUserRequestDto;
import com.prpo.chat.entities.User;
import com.prpo.chat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  @Autowired
  private final UserService userService;

  @PostMapping
  public ResponseEntity<User> createUser(
      @Valid @RequestBody final CreateUserRequestDto request
  ) {
    User createdUser = userService.createUser(request);
    return ResponseEntity.ok(createdUser);
  }
}
