package ru.netology.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.backend.exception.UnauthorizedException;
import ru.netology.backend.model.User;
import ru.netology.backend.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, String> activeToken = new HashMap<>();

    public String login(String login, String password) throws BadRequestException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new BadRequestException(("Invalid login credentials")));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid login credentials");
        }

        String token = tokenService.generateToken(user);
        activeToken.put(token, login);

        return token;
    }

    public void logout(String token) {
        validaToken(token);
        activeToken.remove(token);
    }

    public User getUserByToken(String token) {
        validaToken(token);
        String login = tokenService.getUserLoginFromToken(token);
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private void validaToken(String token) {
        if (!tokenService.validateToken(token) || !activeToken.containsKey(token)) {
            throw new UnauthorizedException(("Invalid or expired token"));
        }
    }
}
