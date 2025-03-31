package ru.netology.backend.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.netology.backend.model.FileMetadata;
import ru.netology.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findBydUser(User user, Pageable pageable);

    Optional<FileMetadata> findByFilenameAndUser(String filename, User user);

    boolean existsByFilenameAndUser(String filename, User user);
}
