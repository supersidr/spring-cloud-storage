package ru.netology.backend.service;

import ru.netology.backend.exception.BadRequestException;
import ru.netology.backend.exception.UnauthorizedException;
import ru.netology.backend.model.User;
import ru.netology.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");
        testUser.setPassword("encoded-password");
    }

    @Test
    void login_ValidCredentials_ReturnsToken() {
        // Arrange
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(tokenService.generateToken(testUser)).thenReturn("token123");

        // Act
        String token = authService.login("testuser", "password");

        // Assert
        assertEquals("token123", token);
        verify(userRepository).findByLogin("testuser");
        verify(passwordEncoder).matches("password", "encoded-password");
        verify(tokenService).generateToken(testUser);
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.login("nonexistent", "password"));
        verify(userRepository).findByLogin("nonexistent");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(tokenService);
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.login("testuser", "wrongpassword"));
        verify(userRepository).findByLogin("testuser");
        verify(passwordEncoder).matches("wrongpassword", "encoded-password");
        verifyNoInteractions(tokenService);
    }

    @Test
    void logout_ValidToken_RemovesToken() {
        // Arrange
        when(tokenService.validateToken("token123")).thenReturn(true);

        // Act
        authService.logout("token123");

        // Assert
        verify(tokenService).validateToken("token123");
    }

    @Test
    void logout_InvalidToken_ThrowsException() {
        // Arrange
        when(tokenService.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.logout("invalid-token"));
        verify(tokenService).validateToken("invalid-token");
    }

    @Test
    void getUserByToken_ValidToken_ReturnsUser() {
        // Arrange
        when(tokenService.validateToken("token123")).thenReturn(true);
        when(tokenService.getUserLoginFromToken("token123")).thenReturn("testuser");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User user = authService.getUserByToken("token123");

        // Assert
        assertEquals(testUser, user);
        verify(tokenService).validateToken("token123");
        verify(tokenService).getUserLoginFromToken("token123");
        verify(userRepository).findByLogin("testuser");
    }

    @Test
    void getUserByToken_InvalidToken_ThrowsException() {
        // Arrange
        when(tokenService.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.getUserByToken("invalid-token"));
        verify(tokenService).validateToken("invalid-token");
        verifyNoMoreInteractions(tokenService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUserByToken_ValidTokenButUserNotFound_ThrowsException() {
        // Arrange
        when(tokenService.validateToken("token123")).thenReturn(true);
        when(tokenService.getUserLoginFromToken("token123")).thenReturn("nonexistent");
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.getUserByToken("token123"));
        verify(tokenService).validateToken("token123");
        verify(tokenService).getUserLoginFromToken("token123");
        verify(userRepository).findByLogin("nonexistent");
    }
}