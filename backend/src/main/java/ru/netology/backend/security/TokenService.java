package ru.netology.backend.security;

import ru.netology.backend.model.entity.TokenEntity;
import ru.netology.backend.model.entity.UserEntity;
import ru.netology.backend.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    @Value("${app.security.token-validity}")
    private long tokenValidityInMilliseconds;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public TokenEntity createToken(UserEntity user) {
        String tokenString = generateTokenString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(tokenValidityInMilliseconds / 1000);

        TokenEntity token = new TokenEntity();
        token.setToken(tokenString);
        token.setUser(user);
        token.setExpiryDate(expiryDate);
        token.setActive(true);

        return tokenRepository.save(token);
    }

    public Optional<TokenEntity> findByToken(String token) {
        return tokenRepository.findByTokenAndActiveTrue(token);
    }

    @Transactional
    public void deactivateToken(String token) {
        tokenRepository.findByTokenAndActiveTrue(token).ifPresent(t -> {
            t.setActive(false);
            tokenRepository.save(t);
        });
    }

    @Transactional
    public void deactivateAllUserTokens(UserEntity user) {
        tokenRepository.deleteByUser(user);
    }

    private String generateTokenString() {
        return UUID.randomUUID().toString();
    }
}