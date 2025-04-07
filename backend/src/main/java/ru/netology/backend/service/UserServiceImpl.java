package ru.netology.backend.service;

import ru.netology.backend.model.dto.LoginDto;
import ru.netology.backend.model.entity.TokenEntity;
import ru.netology.backend.model.entity.UserEntity;
import ru.netology.backend.repository.UserRepository;
import ru.netology.backend.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public String login(LoginDto loginDto) {
//        UserEntity user = userRepository.findByLogin(loginDto.getLogin())
//                .orElseThrow(() -> new BadCredentialsException("Invalid login or password"));
        Optional<UserEntity> userOptional = userRepository.findByLogin(loginDto.getLogin());
        System.out.println("User Optional: " + userOptional);
        UserEntity user = userOptional.orElseThrow(() -> new BadCredentialsException("Invalid login or password"));

        String userPassedEncodedPassword = passwordEncoder.encode(loginDto.getPassword());


        boolean matchesUserPasswordsPassedAndQueried = passwordEncoder.matches(loginDto.getPassword(), userPassedEncodedPassword);
        if (matchesUserPasswordsPassedAndQueried) {
            TokenEntity token = tokenService.createToken(user);
            return token.getToken();
        } else {
            throw new BadCredentialsException("Invalid login or password");
        }
    }

    @Override
    @Transactional
    public void logout(String token) {
        tokenService.deactivateToken(token);
    }

    @Override
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User not authenticated");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }
}
