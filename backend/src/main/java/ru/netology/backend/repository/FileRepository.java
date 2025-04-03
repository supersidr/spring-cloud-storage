package ru.netology.backend.repository;


import org.springframework.stereotype.Repository;
import ru.netology.backend.model.entity.FileEntity;
import ru.netology.backend.model.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUser(UserEntity user, Pageable pageable);
    Optional<FileEntity> findByFilenameAndUser(String filename, UserEntity user);
    boolean existsByFilenameAndUser(String filename, UserEntity user);
}
