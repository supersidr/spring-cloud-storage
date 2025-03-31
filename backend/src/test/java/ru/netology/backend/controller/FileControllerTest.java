package ru.netology.backend.controller;

import ru.netology.backend.model.FileMetadata;
import ru.netology.backend.model.User;
import ru.netology.backend.service.AuthService;
import ru.netology.backend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private FileController fileController;

    private User testUser;
    private String testToken;
    private FileMetadata testFileMetadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        testToken = "test-token-123";

        testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setFilename("test.txt");
        testFileMetadata.setFilePath("/path/to/test.txt");
        testFileMetadata.setSize(1024L);
        testFileMetadata.setHash("abc123");
        testFileMetadata.setCreatedAt(LocalDateTime.now());
        testFileMetadata.setUser(testUser);

        when(authService.getUserByToken(testToken)).thenReturn(testUser);
    }

    @Test
    void getFileList_ReturnsFileList() {
        // Arrange
        List<FileMetadata> files = Arrays.asList(testFileMetadata);
        when(fileService.getFileList(testUser, 10)).thenReturn(files);

        // Act
        ResponseEntity<List<Map<String, Object>>> response = fileController.getFileList(testToken, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test.txt", response.getBody().get(0).get("filename"));
        assertEquals(1024L, response.getBody().get(0).get("size"));

        verify(authService).getUserByToken(testToken);
        verify(fileService).getFileList(testUser, 10);
    }

    @Test
    void uploadFile_Success_ReturnsOk() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        when(fileService.uploadFile(eq(testUser), any(), eq("test.txt"))).thenReturn(testFileMetadata);

        // Act
        ResponseEntity<Void> response = fileController.uploadFile(testToken, "test.txt", file);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getUserByToken(testToken);
        verify(fileService).uploadFile(eq(testUser), any(), eq("test.txt"));
    }

    @Test
    void downloadFile_Success_ReturnsFileResource() throws IOException {
        // Arrange
        Path filePath = Paths.get(testFileMetadata.getFilePath());
        Resource resource = mock(Resource.class);

        when(fileService.downloadFile(testUser, "test.txt")).thenReturn(resource);

        // Act
        ResponseEntity<Resource> response = fileController.downloadFile(testToken, "test.txt");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(resource, response.getBody());
        verify(authService).getUserByToken(testToken);
        verify(fileService).downloadFile(testUser, "test.txt");
    }

    @Test
    void deleteFile_Success_ReturnsOk() throws IOException {
        // Arrange
        doNothing().when(fileService).deleteFile(testUser, "test.txt");

        // Act
        ResponseEntity<Void> response = fileController.deleteFile(testToken, "test.txt");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getUserByToken(testToken);
        verify(fileService).deleteFile(testUser, "test.txt");
    }

    @Test
    void renameFile_Success_ReturnsOk() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("name", "renamed.txt");

        when(fileService.renameFile(testUser, "test.txt", "renamed.txt")).thenReturn(testFileMetadata);

        // Act
        ResponseEntity<Void> response = fileController.renameFile(testToken, "test.txt", request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getUserByToken(testToken);
        verify(fileService).renameFile(testUser, "test.txt", "renamed.txt");
    }
}