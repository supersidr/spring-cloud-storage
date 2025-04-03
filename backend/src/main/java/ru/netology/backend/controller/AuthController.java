package ru.netology.backend.controller;

import ru.netology.backend.model.dto.LoginDto;
import ru.netology.backend.model.dto.LoginResponseDto;
import ru.netology.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto) {
        String token = userService.login(loginDto);
        return ResponseEntity.ok(new LoginResponseDto(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("auth-token") String token) {
        userService.logout(token);
        return ResponseEntity.ok().build();
    }
}
