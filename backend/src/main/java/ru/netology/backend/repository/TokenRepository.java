package ru.netology.backend.repository;

import org.springframework.stereotype.Repository;
import ru.netology.backend.model.entity.TokenEntity;
import ru.netology.backend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
    Optional<TokenEntity> findByTokenAndActiveTrue(String token);
    void deleteByUser(UserEntity user);
}
