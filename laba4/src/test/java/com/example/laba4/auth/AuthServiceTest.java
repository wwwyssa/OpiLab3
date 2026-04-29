package com.example.laba4.auth;

import com.example.laba4.auth.dto.AuthResponse;
import com.example.laba4.auth.dto.LoginRequest;
import com.example.laba4.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success_whenUsernameNotExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testName");
        req.setPassword("123");

        when(userRepository.findByUsername("testName")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123")).thenReturn("hash-123");
        when(userRepository.create("testName", "hash-123")).thenReturn(new UserRepository.UserData(1L, "testName", "hash-123"));
        when(jwtUtil.generateToken("testName")).thenReturn("tok-1");

        AuthResponse res = authService.register(req);

        assertTrue(res.isSuccess());
        assertEquals("testName", res.getUsername());
        assertEquals("Регистрация успешна", res.getMessage());
        assertEquals("tok-1", res.getToken());
        verify(userRepository).create("testName", "hash-123");
    }

    @Test
    void register_fails_whenUsernameExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testName");
        req.setPassword("123");

        when(userRepository.findByUsername("testName"))
                .thenReturn(Optional.of(new UserRepository.UserData(1L, "testName", "h")));

        AuthResponse res = authService.register(req);

        assertFalse(res.isSuccess());
        assertEquals("Пользователь с таким именем уже существует", res.getMessage());
    }

    @Test
    void login_success_withValidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("test");
        req.setPassword("123");

        UserRepository.UserData ud = new UserRepository.UserData(2L, "test", "hashed");
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(ud));
        when(passwordEncoder.matches("123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("test")).thenReturn("tok-2");

        AuthResponse res = authService.login(req);

        assertTrue(res.isSuccess());
        assertEquals("test", res.getUsername());
        assertEquals("Успешный вход", res.getMessage());
        assertEquals("tok-2", res.getToken());
    }

    @Test
    void login_fails_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("такого нет");
        req.setPassword("xD");

        when(userRepository.findByUsername("такого нет")).thenReturn(Optional.empty());

        AuthResponse res = authService.login(req);

        assertFalse(res.isSuccess());
        assertEquals("Неверное имя пользователя или пароль", res.getMessage());
    }

    @Test
    void login_fails_whenPasswordMismatch() {
        LoginRequest req = new LoginRequest();
        req.setUsername("test");
        req.setPassword("wrong");

        UserRepository.UserData ud = new UserRepository.UserData(2L, "test", "hashed");
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(ud));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AuthResponse res = authService.login(req);

        assertFalse(res.isSuccess());
        assertEquals("Неверное имя пользователя или пароль", res.getMessage());
    }
}
