package com.prpo.chat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.prpo.chat.entities.User;
import com.prpo.chat.service.clients.SearchClient;
import com.prpo.chat.service.dtos.FriendshipRequestDto;
import com.prpo.chat.service.dtos.IndexUserRequestDto;
import com.prpo.chat.service.dtos.LoginRequestDto;
import com.prpo.chat.service.dtos.PasswordHashDto;
import com.prpo.chat.service.dtos.RegisterRequestDto;
import com.prpo.chat.service.dtos.UserDto;
import com.prpo.chat.service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private final UserRepository userRepository;

    private final RestTemplate restTemplate;

    private final String ENCRYPTION_SERVICE_URL = "http://localhost:8082/password";

    private final SearchClient searchClient;

    /**
     * Fetches user by id.
     *
     * @param userId the id
     * @return userDto
     * @throws ResponseStatusException with code 404, if user doesn't exist.
     */
    public UserDto getById(final String userId) {
        final var user = getUser(userId);

        final var userDto = new UserDto();
        userDto.setId(userId);
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setProfile(user.getProfile());

        return userDto;
    }

    /**
     * Fetches settings by user id.
     *
     * @param userId the user's id
     * @return the user's settings
     */
    public User.Settings getSettings(final String userId) {
        final var user = getUser(userId);
        return user.getSettings();
    }

    /**
     * Fetches the list of user ids that belong to users who are friends with
     * userId.
     *
     * @param userId the user's id
     * @return the list of friends' ids.
     */
    public List<String> getFriends(final String userId) {
        final var user = getUser(userId);
        return user.getFriends();
    }

    public void setFriends(final FriendshipRequestDto request) {
        final var firstUser = getUser(request.getFirstUserId());
        final var secondUser = getUser(request.getSecondUserId());

        if (firstUser.getFriends() == null) {
            firstUser.setFriends(new ArrayList<>());
        }
        if (secondUser.getFriends() == null) {
            secondUser.setFriends(new ArrayList<>());
        }

        // if users are already friends throw exception
        if (firstUser.getFriends().contains(request.getSecondUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Users are already friends");
        }
        firstUser.getFriends().add(secondUser.getId());
        secondUser.getFriends().add(firstUser.getId());

        userRepository.saveAll(List.of(firstUser, secondUser));
    }

    /**
     * Registers new user, if user with the same username or e-mail doesn't already
     * exist.
     * Hashes the password.
     *
     * @param request email, username(min 5, max 24) and password(min 8, max 128)
     * @return
     */
    @Transactional
    public UserDto registerUser(final RegisterRequestDto request) {
        // TODO: use mappers
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already exists.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already exists.");
        }

        // main info
        var user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        final var hashedPassword = encryptPassword(request.getPassword());
        user.setPasswordHash(hashedPassword);

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

        // friends
        user.setFriends(List.of());

        user = userRepository.save(user);

        final var indexUser = new IndexUserRequestDto();
        indexUser.setId(user.getId());
        indexUser.setUsername(user.getUsername());

        searchClient.indexUser(indexUser);

        final var userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setProfile(user.getProfile());
        return userDto;
    }

    /**
     * Logs user in by confirming user exists and checking if the password matches.
     *
     * @param request includes username and password entered
     * @return UserDto
     */
    public UserDto login(final LoginRequestDto request) {
        final var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        
        if(user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is registered with MetaMask. Please login with your connected wallet.");
        }
        
        final var isCorrect = validatePassword(request.getPassword(), user);

        if (isCorrect) {
            final var userDto = new UserDto();

            userDto.setId(user.getId());
            userDto.setUsername(user.getUsername());
            userDto.setEmail(user.getEmail());
            userDto.setProfile(user.getProfile());

            return userDto;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password incorrect.");
        }
    }

    /**
     * Calls Encryption Service to hash the password.
     *
     * @param password plain String password
     * @return hashed password
     */
    private String encryptPassword(String password) {
        try {
            final var request = new PasswordHashDto();
            request.setPassword(password);

            final var response = restTemplate.postForObject(
                    ENCRYPTION_SERVICE_URL,
                    request,
                    PasswordHashDto.class);

            if (response == null || response.getHashedPassword() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to encrypt password: empty response");
            }
            return response.getHashedPassword();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to encrypt password: ", e);
        }
    }

    /**
     * Calls Encryption service to check if the password matches.
     *
     * @param password plain password
     * @param user     the user, who needed to enter their password
     * @return true if password matches the hashed password that user set.
     */
    private Boolean validatePassword(String password, User user) {
        try {
            final var request = new PasswordHashDto();
            request.setPassword(password);
            request.setHashedPassword(user.getPasswordHash());

            final var response = restTemplate.postForObject(
                    ENCRYPTION_SERVICE_URL + "/validation",
                    request,
                    Boolean.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to validate password: empty response");
            }
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to validate password: ", e);
        }
    }

    private User getUser(final String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("User with id %s not found.", userId)));
    }

    private final SignatureVerificationService signatureVerificationService;

    /**
     * Attempts to log in a user with their wallet.
     * Returns needsRegistration=true if the wallet is not yet registered.
     */
    public com.prpo.chat.service.dtos.WalletLoginResponse loginWithWallet(
            final com.prpo.chat.service.dtos.WalletLoginRequest request) {

        if (!signatureVerificationService.verifySignature(
                request.getMessage(),
                request.getSignature(),
                request.getWalletAddress())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }

        final var userOpt = userRepository.findByWalletAddress(
                request.getWalletAddress().toLowerCase());

        if (userOpt.isEmpty()) {
            return com.prpo.chat.service.dtos.WalletLoginResponse.needsRegistration();
        }

        final var user = userOpt.get();
        final var userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setProfile(user.getProfile());

        return com.prpo.chat.service.dtos.WalletLoginResponse.success(userDto);
    }

    /**
     * Registers a new user with their wallet and chosen username.
     */
    @Transactional
    public UserDto registerWithWallet(
            final com.prpo.chat.service.dtos.WalletRegisterRequest request) {

        if (!signatureVerificationService.verifySignature(
                request.getMessage(),
                request.getSignature(),
                request.getWalletAddress())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }

        final String walletAddress = request.getWalletAddress().toLowerCase();

        if (userRepository.existsByWalletAddress(walletAddress)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        var user = new User();
        user.setUsername(request.getUsername());
        user.setWalletAddress(walletAddress);

        final var profile = new User.Profile();
        user.setProfile(profile);
        profile.setBio("");
        profile.setBirthdate(null);
        profile.setAvatarUrl("https://example.com/avatar.jpg");

        final var settings = new User.Settings();
        user.setSettings(settings);
        settings.setTheme(User.Theme.DARK);
        settings.setNotifications(false);

        user.setFriends(List.of());

        user = userRepository.save(user);
        final var indexUser = new IndexUserRequestDto();
        indexUser.setId(user.getId());
        indexUser.setUsername(user.getUsername());
        searchClient.indexUser(indexUser);

        final var userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setProfile(user.getProfile());
        return userDto;
    }

}
