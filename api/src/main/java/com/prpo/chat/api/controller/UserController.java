package com.prpo.chat.api.controller;

import com.prpo.chat.entities.User;
import com.prpo.chat.service.dtos.FriendshipRequestDto;
import com.prpo.chat.service.dtos.LoginRequestDto;
import com.prpo.chat.service.dtos.RegisterRequestDto;
import com.prpo.chat.service.dtos.UserDto;
import com.prpo.chat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "Operations related to users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "Register user")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User registered successfully."),
			@ApiResponse(responseCode = "409", description = "User already exists."),
			@ApiResponse(responseCode = "500", description = "Failed to encrypt password.")
	})
	@PostMapping("/register")
	public ResponseEntity<UserDto> registerUser(
			@Valid @RequestBody final RegisterRequestDto request) {
		final var createdUser = userService.registerUser(request);
		return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
	}

	@Operation(summary = "Login (with username)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login successful."),
			@ApiResponse(responseCode = "403", description = "Password incorrect."),
			@ApiResponse(responseCode = "404", description = "User not found."),
			@ApiResponse(responseCode = "500", description = "Failed to validate password.")
	})
	@PostMapping("/login")
	public ResponseEntity<UserDto> login(
			@Valid @RequestBody final LoginRequestDto request) {
		final var res = userService.login(request);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@Operation(summary = "Fetches user by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User fetched successfully."),
			@ApiResponse(responseCode = "404", description = "User not found.")
	})
	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUserById(
			@PathVariable("id") String userId) {
		final var res = userService.getById(userId);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@Operation(summary = "Fetches settings of a user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Settings fetched successfully."),
			@ApiResponse(responseCode = "404", description = "User not found.")
	})
	@GetMapping("/settings/{id}")
	public ResponseEntity<User.Settings> getSettings(
			@PathVariable("id") String userId) {
		final var res = userService.getSettings(userId);
		return ResponseEntity.ok(res);
	}

	@Operation(summary = "Fetches a list of userIds of a user's friends")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Friends fetched successfully."),
			@ApiResponse(responseCode = "404", description = "User not found.")
	})
	@GetMapping("/friends/{id}")
	public ResponseEntity<List<String>> getFriends(
			@PathVariable("id") String userId) {
		final var res = userService.getFriends(userId);
		return ResponseEntity.ok(res);
	}

	@Operation(summary = "Establishes a friendship between two users.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Friendship successfully established."),
			@ApiResponse(responseCode = "404", description = "User not found."),
			@ApiResponse(responseCode = "409", description = "Users are already friends.")
	})
	@PostMapping("/friends")
	public ResponseEntity<Void> setFriends(
			@Valid @RequestBody final FriendshipRequestDto request) {
		// TODO: has to have a pending invite
		userService.setFriends(request);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// @Operation(
	// summary = "\"sends\" a friend request from the firstUser to the secondUser"
	// )
	// @ApiResponses({
	// @ApiResponse(responseCode = "201", description = "Friendship successfully
	// established."),
	// @ApiResponse(responseCode = "404", description = "User not found."),
	// @ApiResponse(responseCode = "409", description = "Users are already friends
	// or friend request already sent.")
	// })
	// @PostMapping("/friends/request")
	// public ResponseEntity<Void> sendFriendRequest(
	// @Valid @RequestBody final FriendshipRequestDto request
	// ) {
	// userService.setFriends(request);
	// return new ResponseEntity<>(HttpStatus.OK);
	// }
	// Metamask auth

	@Operation(summary = "Login with Metamask", description = "Authenticates user using Metamask signature. Returns needsRegistration=true if wallet is not registered.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login successful or registration required."),
			@ApiResponse(responseCode = "401", description = "Invalid signature.")
	})
	@PostMapping("/login/wallet")
	public ResponseEntity<com.prpo.chat.service.dtos.WalletLoginResponse> loginWithWallet(
			@Valid @RequestBody final com.prpo.chat.service.dtos.WalletLoginRequest request) {
		final var res = userService.loginWithWallet(request);
		return ResponseEntity.ok(res);
	}

	@Operation(summary = "Register with Metamask", description = "Registers a new user with their Metamask wallet and chosen username.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User registered successfully."),
			@ApiResponse(responseCode = "401", description = "Invalid signature."),
			@ApiResponse(responseCode = "409", description = "Wallet already registered or username taken.")
	})
	@PostMapping("/register/wallet")
	public ResponseEntity<UserDto> registerWithWallet(
			@Valid @RequestBody final com.prpo.chat.service.dtos.WalletRegisterRequest request) {
		final var createdUser = userService.registerWithWallet(request);
		return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
	}
}
