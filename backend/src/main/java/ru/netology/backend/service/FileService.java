package ru.netology.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.backend.config.AppConfig;
import ru.netology.backend.exception.BadRequestException;
import ru.netology.backend.model.FileMetadata;
import ru.netology.backend.model.User;
import ru.netology.backend.repository.FileMetadataRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final FileMetadataRepository fileMetadataRepository;
    private final AppConfig.AppProperties appProperties;

    public List<FileMetadata> getFileList(User user, Integer limit) {
        int pageSize = limit != null && limit > 0 ? limit : Integer.MAX_VALUE;
        return fileMetadataRepository.findBydUser(user, PageRequest.of(0, pageSize));
    }

    public FileMetadata uploadFile(User user, MultipartFile file, String filename) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (filename == null || filename.trim().isEmpty()) {
            filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                throw new BadRequestException("Filename is required");
            }
        }

        if (fileMetadataRepository.existsByFilenameAndUser(filename, user)) {
            throw new BadRequestException("File already " + filename + " exists");
        }

        String hash = DigestUtils.md5DigestAsHex(file.getInputStream()).toUpperCase();

        String uniqueFileName = UUID.randomUUID() + "." + FilenameUtils.getExtension(filename);
        Path storagePath = Paths.get(appProperties.getStorage().getLocation(), uniqueFileName);

        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

        FileMetadata metadata = new FileMetadata();
        metadata.setFilename(filename);
        metadata.setFilePath(storagePath.toString());
        metadata.setSize(file.getSize());
        metadata.setHash(hash);
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setUser(user);

        return fileMetadataRepository.save(metadata);
    }

    public Resource downloadFile(User user, String filename) throws IOException {
        FileMetadata metadata = getFileMetadata(user, filename);
        Path filePath = Paths.get(metadata.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Could not read the file");
        }
        return resource;
    }

    public void deleteFile(User user, String filename) throws IOException {
        FileMetadata metadata = getFileMetadata(user, filename);
        Path filePath = Paths.get(metadata.getFilePath());

        boolean deleted = Files.deleteIfExists(filePath);
        if (!deleted) {
            throw new IOException("Could not delete the file");
        }

        fileMetadataRepository.delete(metadata);
    }

    public FileMetadata renameFile(User user, String oldFilename, String newFilename) throws IOException {
        if (newFilename == null || oldFilename.trim().isEmpty()) {
            throw new BadRequestException("Filename is required");
        }

        if (fileMetadataRepository.existsByFilenameAndUser(newFilename, user)) {
            throw new BadRequestException("File with name '" + newFilename + "' already exists");
        }

        FileMetadata metadata = getFileMetadata(user, oldFilename);
        metadata.setFilename(newFilename);

        return fileMetadataRepository.save(metadata);
    }

    private FileMetadata getFileMetadata(User user, String filename) {
        return fileMetadataRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> new BadRequestException("File '" + filename + "' not found"));
    }
}
