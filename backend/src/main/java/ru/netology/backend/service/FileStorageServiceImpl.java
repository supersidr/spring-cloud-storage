package ru.netology.backend.service;

import ru.netology.backend.exception.FileStorageException;
import ru.netology.backend.model.dto.FileDto;
import ru.netology.backend.model.entity.FileEntity;
import ru.netology.backend.model.entity.UserEntity;
import ru.netology.backend.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final FileRepository fileRepository;
    private final UserService userService;

    public FileStorageServiceImpl(
            @Value("${app.storage.location}") String uploadDir,
            FileRepository fileRepository,
            UserService userService) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;
        this.userService = userService;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    @Transactional
    public FileEntity storeFile(MultipartFile file, String filename) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file");
        }

        UserEntity currentUser = userService.getCurrentUser();

        // Normalize filename
        String normalizedFilename = StringUtils.cleanPath(Objects.requireNonNull(
                filename != null && !filename.isEmpty() ? filename : file.getOriginalFilename()));

        // Check if the filename contains invalid characters
        if (normalizedFilename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence " + normalizedFilename);
        }

        // Check if file with this name already exists for this user
        if (fileRepository.existsByFilenameAndUser(normalizedFilename, currentUser)) {
            throw new FileStorageException("A file with this name already exists");
        }

        // Generate a unique filename for storage
        String storageFilename = UUID.randomUUID().toString();

        try {
            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(storageFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Save file metadata in database
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(normalizedFilename);
            fileEntity.setStorageFilename(storageFilename);
            fileEntity.setSize(file.getSize());
            fileEntity.setUser(currentUser);

            return fileRepository.save(fileEntity);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + normalizedFilename, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        UserEntity currentUser = userService.getCurrentUser();

        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, currentUser)
                .orElseThrow(() -> new FileStorageException("File not found: " + filename));

        try {
            Path filePath = this.fileStorageLocation.resolve(fileEntity.getStorageFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + filename, ex);
        }
    }

    @Override
    public List<FileDto> getFilesList(Integer limit) {
        UserEntity currentUser = userService.getCurrentUser();

        Pageable pageable = limit != null ? PageRequest.of(0, limit) : Pageable.unpaged();

        return fileRepository.findByUser(currentUser, pageable).stream()
                .map(fileEntity -> new FileDto(fileEntity.getFilename(), fileEntity.getSize()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFile(String filename) {
        UserEntity currentUser = userService.getCurrentUser();

        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, currentUser)
                .orElseThrow(() -> new FileStorageException("File not found: " + filename));

        try {
            // Delete file from storage
            Path filePath = this.fileStorageLocation.resolve(fileEntity.getStorageFilename());
            Files.deleteIfExists(filePath);

            // Delete file metadata from database
            fileRepository.delete(fileEntity);
        } catch (IOException ex) {
            throw new FileStorageException("Error deleting file: " + filename, ex);
        }
    }

    @Override
    @Transactional
    public void renameFile(String oldFilename, String newFilename) {
        if (newFilename == null || newFilename.trim().isEmpty()) {
            throw new FileStorageException("New filename cannot be empty");
        }

        String normalizedNewFilename = StringUtils.cleanPath(newFilename);

        if (normalizedNewFilename.contains("..")) {
            throw new FileStorageException("New filename contains invalid path sequence " + normalizedNewFilename);
        }

        UserEntity currentUser = userService.getCurrentUser();

        // Check if a file with the new name already exists
        if (fileRepository.existsByFilenameAndUser(normalizedNewFilename, currentUser)) {
            throw new FileStorageException("A file with name " + normalizedNewFilename + " already exists");
        }

        FileEntity fileEntity = fileRepository.findByFilenameAndUser(oldFilename, currentUser)
                .orElseThrow(() -> new FileStorageException("File not found: " + oldFilename));

        fileEntity.setFilename(normalizedNewFilename);
        fileRepository.save(fileEntity);
    }
}
