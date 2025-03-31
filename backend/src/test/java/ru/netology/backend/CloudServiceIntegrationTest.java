package ru.netology.backend;

import ru.netology.backend.model.User;
import ru.netology.backend.repository.FileMetadataRepository;
import ru.netology.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CloudServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("cloudservice_test_db")
            .withUsername("postgres")
            .withPassword("postgres");

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
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String authToken;
    private Path storageDirectory;

    @BeforeEach
    void setUp() throws Exception {
        // Clear repositories
        fileMetadataRepository.deleteAll();
        userRepository.deleteAll();

        // Create storage directory
        storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "cloud-service-test");
        Files.createDirectories(storageDirectory);

        // Create test user
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(testUser);

        // Login and get auth token
        String loginJson = "{\"login\":\"testuser\",\"password\":\"password\"}";

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        authToken = jsonNode.get("auth-token").asText();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up files
        if (Files.exists(storageDirectory)) {
            Files.walk(storageDirectory)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(storageDirectory);
        }
    }

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        String loginJson = "{\"login\":\"testuser\",\"password\":\"password\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").exists())
                .andExpect(jsonPath("$.auth-token").isString());
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        String loginJson = "{\"login\":\"testuser\",\"password\":\"wrongpassword\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_ValidToken_ReturnsOk() throws Exception {
        mockMvc.perform(post("/logout")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());
    }

    @Test
    void logout_InvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/logout")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadAndDownloadFile_Success() throws Exception {
        // Upload file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/file")
                        .file(file)
                        .param("filename", "test.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Verify file is in repository
        assertEquals(1, fileMetadataRepository.findByUser(testUser, null).size());

        // Download file
        MvcResult downloadResult = mockMvc.perform(get("/file")
                        .param("filename", "test.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("test.txt")))
                .andReturn();

        // Verify content
        byte[] content = downloadResult.getResponse().getContentAsByteArray();
        assertEquals("Hello, World!", new String(content));
    }

    @Test
    void listFiles_ReturnsFileList() throws Exception {
        // Upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/file")
                        .file(file)
                        .param("filename", "test.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // List files
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("test.txt")))
                .andExpect(jsonPath("$[0].size", greaterThan(0)));
    }

    @Test
    void deleteFile_Success() throws Exception {
        // Upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-delete.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/file")
                        .file(file)
                        .param("filename", "test-delete.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Verify file is in repository
        assertEquals(1, fileMetadataRepository.findByUser(testUser, null).size());

        // Delete file
        mockMvc.perform(delete("/file")
                        .param("filename", "test-delete.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Verify file is deleted
        assertEquals(0, fileMetadataRepository.findByUser(testUser, null).size());
    }

    @Test
    void renameFile_Success() throws Exception {
        // Upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/file")
                        .file(file)
                        .param("filename", "original.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Rename file
        String renameJson = "{\"name\":\"renamed.txt\"}";

        mockMvc.perform(put("/file")
                        .param("filename", "original.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameJson)
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Verify file is renamed
        assertEquals(1, fileMetadataRepository.findByUser(testUser, null).size());
        assertEquals("renamed.txt", fileMetadataRepository.findByUser(testUser, null).get(0).getFilename());
    }
}
