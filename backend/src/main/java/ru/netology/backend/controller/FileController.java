package ru.netology.backend.controller;

import ru.netology.backend.model.FileMetadata;
import ru.netology.backend.model.User;
import ru.netology.backend.service.AuthService;
import ru.netology.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;
    private final AuthService authService;

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getFileList(
            @RequestHeader("auth-token") String token,
            @RequestParam(required = false) Integer limit) {

        User user = authService.getUserByToken(token);
        List<FileMetadata> files = fileService.getFileList(user, limit);

//        List<Map<String, Object>> response = files.stream()
//                .map(file -> Map.of(
//                        "filename", file.getFilename(),
//                        "size", file.getSize()
//                )).collect(Collectors.toList());
        List<Map<String, Object>> response = files.stream()
                .map(file -> {
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("filename", file.getFilename());
                    fileMap.put("size", file.getSize());
                    return fileMap;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/file")
    public ResponseEntity<Void> UploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam(required = false) String filename,
            @RequestParam("file") MultipartFile file) throws IOException {
        User user = authService.getUserByToken(token);
        fileService.uploadFile(user, file, filename);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam(required = false) String filename
    ) throws IOException {
        User user = authService.getUserByToken(token);
        Resource resource = fileService.downloadFile(user, filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam(required = false) String filename
    ) throws IOException {
        User user = authService.getUserByToken(token);
        fileService.deleteFile(user, filename);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam String filename,
            @RequestBody Map<String, Object> request
    ) throws IOException {
        String newFilename = (String) request.get("name");
        User user = authService.getUserByToken(token);
        fileService.renameFile(user, filename, newFilename);

        return ResponseEntity.ok().build();
    }
}
