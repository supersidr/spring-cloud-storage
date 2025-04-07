package ru.netology.backend.service;

import org.springframework.stereotype.Service;
import ru.netology.backend.model.dto.LoginDto;
import ru.netology.backend.model.entity.UserEntity;

import java.util.Optional;

public interface UserService {
    String login(LoginDto loginDto);
    void logout(String token);
    UserEntity getCurrentUser();
}