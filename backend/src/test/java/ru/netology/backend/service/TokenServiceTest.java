package ru.netology.backend.service;

import ru.netology.backend.config.AppConfig;
import ru.netology.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    @Mock
    private AppConfig.AppProperties appProperties;

    @Mock
    private AppConfig.AppProperties.Security security;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        when(appProperties.getSecurity()).thenReturn(security);
        when(security.getTokenSecret()).thenReturn("test-secret-key-that-is-at-least-32-characters-long");
        when(security.getTokenExpirationMs()).thenReturn(3600000L); // 1 hour
    }

    @Test
    void generateToken_Success_ReturnsValidToken() {
        // Act
        String token = tokenService.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);

        verify(appProperties, atLeastOnce()).getSecurity();
        verify(security).getTokenSecret();
        verify(security).getTokenExpirationMs();
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = tokenService.generateToken(testUser);

        // Act
        boolean isValid = tokenService.validateToken(token);

        // Assert
        assertTrue(isValid);

        verify(appProperties, atLeastOnce()).getSecurity();
        verify(security, atLeastOnce()).getTokenSecret();
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act
        boolean isValid = tokenService.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);

        verify(appProperties, atLeastOnce()).getSecurity();
        verify(security, atLeastOnce()).getTokenSecret();
    }

    @Test
    void getUserLoginFromToken_ValidToken_ReturnsLogin() {
        // Arrange
        String token = tokenService.generateToken(testUser);

        // Act
        String login = tokenService.getUserLoginFromToken(token);

        // Assert
        assertEquals("testuser", login);

        verify(appProperties, atLeastOnce()).getSecurity();
        verify(security, atLeastOnce()).getTokenSecret();
    }
}