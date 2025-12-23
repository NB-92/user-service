package com.prpo.chat.api.controller;

import com.prpo.chat.service.dtos.RegisterRequestDto;
import com.prpo.chat.service.dtos.UserDto;
import com.prpo.chat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "Operations related to users")
public class UserController {

  @Autowired
  private final UserService userService;

  @Operation(
          summary = "Register user"
  )
  @PostMapping("/register")
  public ResponseEntity<UserDto> registerUser(
      @Valid @RequestBody final RegisterRequestDto request
  ) {
    final var createdUser = userService.registerUser(request);
    return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
  }
}
