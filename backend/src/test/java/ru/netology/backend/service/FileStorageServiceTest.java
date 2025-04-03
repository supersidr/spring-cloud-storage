package ru.netology.backend.service;

import ru.netology.backend.exception.FileStorageException;
import ru.netology.backend.model.dto.FileDto;
import ru.netology.backend.model.entity.FileEntity;
import ru.netology.backend.model.entity.UserEntity;
import ru.netology.backend.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private UserEntity testUser;
    private FileEntity testFile;
    private Path testStorageLocation;

    @BeforeEach
    void setUp() throws IOException {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setFilename("test.txt");
        testFile.setStorageFilename("uuid-test.txt");
        testFile.setSize(100L);
        testFile.setUser(testUser);

        // Create temporary directory for tests
        testStorageLocation = Files.createTempDirectory("test-uploads");
        ReflectionTestUtils.setField(fileStorageService, "fileStorageLocation", testStorageLocation);

        when(userService.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void getFilesListShouldReturnFilesList() {
        // Given
        Integer limit = 10;
        Pageable pageable = PageRequest.of(0, limit);

        FileEntity file1 = new FileEntity(1L, "file1.txt", "uuid1", 100L, testUser);
        FileEntity file2 = new FileEntity(2L, "file2.txt", "uuid2", 200L, testUser);

        when(fileRepository.findByUser(testUser, pageable)).thenReturn(Arrays.asList(file1, file2));

        // When
        List<FileDto> result = fileStorageService.getFilesList(limit);

        // Then
        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getFilename());
        assertEquals(100L, result.get(0).getSize());
        assertEquals("file2.txt", result.get(1).getFilename());
        assertEquals(200L, result.get(1).getSize());
    }

    @Test
    void storeFileShouldSaveFile() throws IOException {
        // Given
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "original.txt",
                "text/plain",
                "test content".getBytes()
        );

        when(fileRepository.existsByFilenameAndUser(anyString(), any(UserEntity.class))).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFile);

        // When
        FileEntity result = fileStorageService.storeFile(multipartFile, "test.txt");

        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getFilename());
        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void storeFileShouldThrowExceptionIfFileExists() {
        // Given
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "original.txt",
                "text/plain",
                "test content".getBytes()
        );

        when(fileRepository.existsByFilenameAndUser("test.txt", testUser)).thenReturn(true);

        // When & Then
        assertThrows(FileStorageException.class, () ->
                fileStorageService.storeFile(multipartFile, "test.txt")
        );

        verify(fileRepository, never()).save(any(FileEntity.class));
    }

    @Test
    void deleteFileShouldRemoveFile() throws IOException {
        // Given
        String filename = "test.txt";

        // Create a test file on disk
        Path testFilePath = testStorageLocation.resolve(testFile.getStorageFilename());
        Files.createFile(testFilePath);

        when(fileRepository.findByFilenameAndUser(filename, testUser)).thenReturn(Optional.of(testFile));

        // When
        fileStorageService.deleteFile(filename);

        // Then
        verify(fileRepository).delete(testFile);
        assertFalse(Files.exists(testFilePath), "File should be deleted from disk");
    }

    @Test
    void deleteFileShouldThrowExceptionIfFileNotFound() {
        // Given
        String filename = "nonexistent.txt";

        when(fileRepository.findByFilenameAndUser(filename, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(FileStorageException.class, () ->
                fileStorageService.deleteFile(filename)
        );

        verify(fileRepository, never()).delete(any(FileEntity.class));
    }

    @Test
    void renameFileShouldUpdateFilename() {
        // Given
        String oldFilename = "old.txt";
        String newFilename = "new.txt";

        when(fileRepository.findByFilenameAndUser(oldFilename, testUser)).thenReturn(Optional.of(testFile));
        when(fileRepository.existsByFilenameAndUser(newFilename, testUser)).thenReturn(false);

        // When
        fileStorageService.renameFile(oldFilename, newFilename);

        // Then
        assertEquals(newFilename, testFile.getFilename());
        verify(fileRepository).save(testFile);
    }

    @Test
    void renameFileShouldThrowExceptionIfNewFilenameExists() {
        // Given
        String oldFilename = "old.txt";
        String newFilename = "existing.txt";

        when(fileRepository.findByFilenameAndUser(oldFilename, testUser)).thenReturn(Optional.of(testFile));
        when(fileRepository.existsByFilenameAndUser(newFilename, testUser)).thenReturn(true);

        // When & Then
        assertThrows(FileStorageException.class, () ->
                fileStorageService.renameFile(oldFilename, newFilename)
        );

        verify(fileRepository, never()).save(any(FileEntity.class));
    }
}