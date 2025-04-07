package ru.netology.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.netology.backend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByLogin(String login);
//@Query("SELECT u FROM UserEntity u WHERE LOWER(u.login) = LOWER(:login)")
//Optional<UserEntity> findByLogin(@Param("login") String login);
}
