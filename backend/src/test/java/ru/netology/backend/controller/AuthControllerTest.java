package ru.netology.backend.controller;

import ru.netology.backend.exception.BadRequestException;
import ru.netology.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_ValidCredentials_ReturnsToken() {
        // Arrange
        Map<String, String> credentials = new HashMap<>();
        credentials.put("login", "user");
        credentials.put("password", "password");

        when(authService.login("user", "password")).thenReturn("token123");

        // Act
        ResponseEntity<Map<String, String>> response = authController.login(credentials);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("token123", response.getBody().get("auth-token"));
        verify(authService).login("user", "password");
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        Map<String, String> credentials = new HashMap<>();
        credentials.put("login", "user");
        credentials.put("password", "wrongpassword");

        when(authService.login("user", "wrongpassword")).thenThrow(new BadRequestException("Invalid login credentials"));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authController.login(credentials));
        verify(authService).login("user", "wrongpassword");
    }

    @Test
    void logout_ValidToken_ReturnsOk() {
        // Arrange
        String token = "valid-token";
        doNothing().when(authService).logout(token);

        // Act
        ResponseEntity<Void> response = authController.logout(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout(token);
    }
}