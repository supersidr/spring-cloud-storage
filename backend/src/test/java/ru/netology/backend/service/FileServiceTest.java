package ru.netology.backend.service;

import ru.netology.backend.config.AppConfig;
import ru.netology.backend.exception.BadRequestException;
import ru.netology.backend.model.FileMetadata;
import ru.netology.backend.model.User;
import ru.netology.backend.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private AppConfig.AppProperties appProperties;

    @Mock
    private AppConfig.AppProperties.Storage storage;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private User testUser;
    private FileMetadata testFileMetadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setFilename("test.txt");
        testFileMetadata.setFilePath(tempDir.resolve("test.txt").toString());
        testFileMetadata.setSize(1024L);
        testFileMetadata.setHash("abc123");
        testFileMetadata.setCreatedAt(LocalDateTime.now());
        testFileMetadata.setUser(testUser);

        when(appProperties.getStorage()).thenReturn(storage);
        when(storage.getLocation()).thenReturn(tempDir.toString());
    }

    @Test
    void getFileList_ReturnsFiles() {
        // Arrange
        List<FileMetadata> expectedFiles = Arrays.asList(testFileMetadata);
        when(fileMetadataRepository.findByUser(eq(testUser), any())).thenReturn(expectedFiles);

        // Act
        List<FileMetadata> files = fileService.getFileList(testUser, 10);

        // Assert
        assertEquals(expectedFiles, files);
        verify(fileMetadataRepository).findByUser(eq(testUser), any());
    }

    @Test
    void uploadFile_Success_ReturnsFileMetadata() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        when(fileMetadataRepository.existsByFilenameAndUser("test.txt", testUser)).thenReturn(false);
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        FileMetadata result = fileService.uploadFile(testUser, file, "test.txt");

        // Assert
        assertNotNull(result);
        assertEquals("test.txt", result.getFilename());
        assertEquals(testUser, result.getUser());
        assertTrue(result.getFilePath().contains(tempDir.toString()));

        verify(fileMetadataRepository).existsByFilenameAndUser("test.txt", testUser);
        verify(fileMetadataRepository).save(any(FileMetadata.class));
    }

    @Test
    void uploadFile_EmptyFile_ThrowsException() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Act & Assert
        assertThrows(BadRequestException.class, () -> fileService.uploadFile(testUser, emptyFile, "empty.txt"));
        verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    void uploadFile_FileAlreadyExists_ThrowsException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "existing.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        when(fileMetadataRepository.existsByFilenameAndUser("existing.txt", testUser)).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> fileService.uploadFile(testUser, file, "existing.txt"));
        verify(fileMetadataRepository).existsByFilenameAndUser("existing.txt", testUser);
        verifyNoMoreInteractions(fileMetadataRepository);
    }

    @Test
    void downloadFile_Success_ReturnsResource() throws IOException {
        // Arrange
        Path filePath = tempDir.resolve("test.txt");
        Files.write(filePath, "Hello, World!".getBytes());

        testFileMetadata.setFilePath(filePath.toString());

        when(fileMetadataRepository.findByFilenameAndUser("test.txt", testUser))
                .thenReturn(Optional.of(testFileMetadata));

        // Act
        Resource resource = fileService.downloadFile(testUser, "test.txt");

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());

        verify(fileMetadataRepository).findByFilenameAndUser("test.txt", testUser);
    }

    @Test
    void downloadFile_FileNotFound_ThrowsException() {
        // Arrange
        when(fileMetadataRepository.findByFilenameAndUser("nonexistent.txt", testUser))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> fileService.downloadFile(testUser, "nonexistent.txt"));
        verify(fileMetadataRepository).findByFilenameAndUser("nonexistent.txt", testUser);
    }

    @Test
    void deleteFile_Success() throws IOException {
        // Arrange
        Path filePath = tempDir.resolve("test-delete.txt");
        Files.write(filePath, "Hello, World!".getBytes());

        FileMetadata metadata = new FileMetadata();
        metadata.setFilePath(filePath.toString());

        when(fileMetadataRepository.findByFilenameAndUser("test-delete.txt", testUser))
                .thenReturn(Optional.of(metadata));

        // Act
        fileService.deleteFile(testUser, "test-delete.txt");

        // Assert
        assertFalse(Files.exists(filePath));
        verify(fileMetadataRepository).findByFilenameAndUser("test-delete.txt", testUser);
        verify(fileMetadataRepository).delete(metadata);
    }

    @Test
    void renameFile_Success_ReturnsUpdatedMetadata() {
        // Arrange
        when(fileMetadataRepository.findByFilenameAndUser("old.txt", testUser))
                .thenReturn(Optional.of(testFileMetadata));
        when(fileMetadataRepository.existsByFilenameAndUser("new.txt", testUser))
                .thenReturn(false);
        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenReturn(testFileMetadata);

        // Act
        FileMetadata result = fileService.renameFile(testUser, "old.txt", "new.txt");

        // Assert
        assertNotNull(result);
        assertEquals("new.txt", result.getFilename());

        verify(fileMetadataRepository).findByFilenameAndUser("old.txt", testUser);
        verify(fileMetadataRepository).existsByFilenameAndUser("new.txt", testUser);
        verify(fileMetadataRepository).save(testFileMetadata);
    }

    @Test
    void renameFile_NewNameAlreadyExists_ThrowsException() {
        // Arrange
        when(fileMetadataRepository.findByFilenameAndUser("old.txt", testUser))
                .thenReturn(Optional.of(testFileMetadata));
        when(fileMetadataRepository.existsByFilenameAndUser("existing.txt", testUser))
                .thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> fileService.renameFile(testUser, "old.txt", "existing.txt"));

        verify(fileMetadataRepository).findByFilenameAndUser("old.txt", testUser);
        verify(fileMetadataRepository).existsByFilenameAndUser("existing.txt", testUser);
        verifyNoMoreInteractions(fileMetadataRepository);
    }
}