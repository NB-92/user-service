package com.prpo.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
import com.prpo.chat.service.dtos.WalletLoginRequest;
import com.prpo.chat.service.dtos.WalletLoginResponse;
import com.prpo.chat.service.dtos.WalletRegisterRequest;
import com.prpo.chat.service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String ENCRYPT_URL = "http://encryption:8082/password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SearchClient searchClient;

    @Mock
    private SignatureVerificationService signatureVerificationService;

    @InjectMocks
    private UserService service;

    @Test
    void registerUser_savesDefaultsAndIndexes() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("user@example.com");
        request.setUsername("user-1");
        request.setPassword("password-123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("user-1")).thenReturn(false);

        PasswordHashDto hashResponse = new PasswordHashDto();
        hashResponse.setHashedPassword("hashed");
        when(restTemplate.postForObject(eq(ENCRYPT_URL), any(PasswordHashDto.class), eq(PasswordHashDto.class)))
            .thenReturn(hashResponse);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("user-123");
            return saved;
        });

        UserDto result = service.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("user-1", saved.getUsername());
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("hashed", saved.getPasswordHash());
        assertNotNull(saved.getProfile());
        assertEquals("https://example.com/avatar.jpg", saved.getProfile().getAvatarUrl());
        assertNotNull(saved.getSettings());
        assertEquals(User.Theme.DARK, saved.getSettings().getTheme());
        assertTrue(saved.getSettings().isNotifications());
        assertEquals(List.of(), saved.getFriends());

        ArgumentCaptor<IndexUserRequestDto> indexCaptor = ArgumentCaptor.forClass(IndexUserRequestDto.class);
        verify(searchClient).indexUser(indexCaptor.capture());
        IndexUserRequestDto index = indexCaptor.getValue();
        assertEquals("user-123", index.getId());
        assertEquals("user-1", index.getUsername());

        assertEquals("user-123", result.getId());
        assertEquals("user-1", result.getUsername());
        assertEquals("user@example.com", result.getEmail());
        assertEquals(saved.getProfile(), result.getProfile());
    }

    @Test
    void registerUser_throwsWhenEmailExists() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("user@example.com");
        request.setUsername("user-1");
        request.setPassword("password-123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registerUser(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verifyNoInteractions(restTemplate, searchClient);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_throwsWhenUsernameExists() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("user@example.com");
        request.setUsername("user-1");
        request.setPassword("password-123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("user-1")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registerUser(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verifyNoInteractions(restTemplate, searchClient);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success_whenPasswordValid() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("user-1");
        request.setPassword("password-123");

        User user = new User();
        user.setId("user-1");
        user.setUsername("user-1");
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        User.Profile profile = new User.Profile();
        profile.setAvatarUrl("avatar");
        user.setProfile(profile);

        when(userRepository.findByUsername("user-1")).thenReturn(Optional.of(user));
        when(restTemplate.postForObject(eq(ENCRYPT_URL + "/validation"), any(PasswordHashDto.class), eq(Boolean.class)))
            .thenReturn(true);

        UserDto result = service.login(request);

        assertEquals("user-1", result.getId());
        assertEquals("user-1", result.getUsername());
        assertEquals("user@example.com", result.getEmail());
        assertEquals(profile, result.getProfile());

        ArgumentCaptor<PasswordHashDto> passwordCaptor = ArgumentCaptor.forClass(PasswordHashDto.class);
        verify(restTemplate).postForObject(eq(ENCRYPT_URL + "/validation"), passwordCaptor.capture(), eq(Boolean.class));
        assertEquals("password-123", passwordCaptor.getValue().getPassword());
        assertEquals("hashed", passwordCaptor.getValue().getHashedPassword());
    }

    @Test
    void login_throwsWhenPasswordMissingForWalletUser() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("user-1");
        request.setPassword("password-123");

        User user = new User();
        user.setUsername("user-1");
        user.setPasswordHash(null);

        when(userRepository.findByUsername("user-1")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void login_throwsWhenPasswordInvalid() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("user-1");
        request.setPassword("password-123");

        User user = new User();
        user.setUsername("user-1");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("user-1")).thenReturn(Optional.of(user));
        when(restTemplate.postForObject(eq(ENCRYPT_URL + "/validation"), any(PasswordHashDto.class), eq(Boolean.class)))
            .thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void setFriends_addsAndSavesBothUsers() {
        User first = new User();
        first.setId("user-1");
        first.setFriends(null);

        User second = new User();
        second.setId("user-2");
        second.setFriends(null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(first));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(second));

        FriendshipRequestDto request = new FriendshipRequestDto();
        request.setFirstUserId("user-1");
        request.setSecondUserId("user-2");

        service.setFriends(request);

        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());
        List<User> saved = captor.getValue();

        User savedFirst = saved.stream()
            .filter(user -> "user-1".equals(user.getId()))
            .findFirst()
            .orElseThrow();
        User savedSecond = saved.stream()
            .filter(user -> "user-2".equals(user.getId()))
            .findFirst()
            .orElseThrow();

        assertEquals(List.of("user-2"), savedFirst.getFriends());
        assertEquals(List.of("user-1"), savedSecond.getFriends());
    }

    @Test
    void loginWithWallet_needsRegistrationWhenUserMissing() {
        WalletLoginRequest request = new WalletLoginRequest();
        request.setWalletAddress("0xABC");
        request.setMessage("message");
        request.setSignature("signature");

        when(signatureVerificationService.verifySignature("message", "signature", "0xABC"))
            .thenReturn(true);
        when(userRepository.findByWalletAddress("0xabc")).thenReturn(Optional.empty());

        WalletLoginResponse response = service.loginWithWallet(request);

        assertTrue(response.isNeedsRegistration());
        assertNull(response.getUser());
    }

    @Test
    void registerWithWallet_savesAndIndexesLowercasedWallet() {
        WalletRegisterRequest request = new WalletRegisterRequest();
        request.setWalletAddress("0xABCDEF");
        request.setMessage("message");
        request.setSignature("signature");
        request.setUsername("wallet-user");

        when(signatureVerificationService.verifySignature("message", "signature", "0xABCDEF"))
            .thenReturn(true);
        when(userRepository.existsByWalletAddress("0xabcdef")).thenReturn(false);
        when(userRepository.existsByUsername("wallet-user")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("user-500");
            return saved;
        });

        UserDto result = service.registerWithWallet(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("wallet-user", saved.getUsername());
        assertEquals("0xabcdef", saved.getWalletAddress());
        assertNotNull(saved.getProfile());
        assertNotNull(saved.getSettings());
        assertEquals(User.Theme.DARK, saved.getSettings().getTheme());
        assertFalse(saved.getSettings().isNotifications());
        assertEquals(List.of(), saved.getFriends());

        ArgumentCaptor<IndexUserRequestDto> indexCaptor = ArgumentCaptor.forClass(IndexUserRequestDto.class);
        verify(searchClient).indexUser(indexCaptor.capture());
        IndexUserRequestDto index = indexCaptor.getValue();
        assertEquals("user-500", index.getId());
        assertEquals("wallet-user", index.getUsername());

        assertEquals("user-500", result.getId());
        assertEquals("wallet-user", result.getUsername());
        assertEquals(saved.getProfile(), result.getProfile());
    }
}
