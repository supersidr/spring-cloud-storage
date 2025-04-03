package ru.netology.backend.controller;

import ru.netology.backend.model.dto.LoginDto;
import ru.netology.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto("user", "password");
        String token = "valid-token";

        when(userService.login(any(LoginDto.class))).thenReturn(token);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").value(token));
    }

    @Test
    void loginShouldReturn400WhenCredentialsAreInvalid() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto("user", "wrong-password");

        when(userService.login(any(LoginDto.class))).thenThrow(new BadCredentialsException("Invalid login or password"));

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid login or password"));
    }

    @Test
    void logoutShouldDeactivateToken() throws Exception {
        // Given
        String token = "valid-token";

        doNothing().when(userService).logout(token);

        // When & Then
        mockMvc.perform(post("/logout")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        verify(userService).logout(token);
    }
}