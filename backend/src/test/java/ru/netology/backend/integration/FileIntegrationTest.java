package ru.netology.backend.integration;

import ru.netology.backend.model.dto.LoginDto;
import ru.netology.backend.model.dto.LoginResponseDto;
import ru.netology.backend.model.dto.RenameFileDto;
import ru.netology.backend.model.entity.UserEntity;
import ru.netology.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FileIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create test user
        UserEntity user = new UserEntity();
        user.setLogin("testuser");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);

        // Login to get auth token
        LoginDto loginDto = new LoginDto("testuser", "password");
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto responseDto = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponseDto.class);
        authToken = responseDto.getAuthToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void fileUploadDownloadDeleteTest() throws Exception {
        // Create test file content
        String fileContent = "Test file content for integration test";
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, fileContent.getBytes());

        // Upload file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                Files.readAllBytes(tempFile));

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "test.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Check file is in the list
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename").value("test.txt"));

        // Rename file
        RenameFileDto renameFileDto = new RenameFileDto("renamed.txt");
        mockMvc.perform(put("/file")
                        .param("filename", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameFileDto))
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Check renamed file is in the list
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename").value("renamed.txt"));

        // Download file
        MvcResult downloadResult = mockMvc.perform(get("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"renamed.txt\""))
                .andReturn();

        String downloadedContent = downloadResult.getResponse().getContentAsString();
        assertTrue(downloadedContent.equals(fileContent), "Downloaded content should match uploaded content");

        // Delete file
        mockMvc.perform(delete("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Check file list is empty
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}