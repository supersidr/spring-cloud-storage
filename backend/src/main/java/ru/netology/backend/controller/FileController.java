package ru.netology.backend.controller;

import ru.netology.backend.model.dto.FileDto;
import ru.netology.backend.model.dto.RenameFileDto;
import ru.netology.backend.model.entity.FileEntity;
import ru.netology.backend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/file")
    public ResponseEntity<Void> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {

        FileEntity savedFile = fileStorageService.storeFile(file, filename);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filename") String filename) {
        Resource resource = fileStorageService.loadFileAsResource(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(@RequestParam("filename") String filename) {
        fileStorageService.deleteFile(filename);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @RequestParam("filename") String filename,
            @RequestBody RenameFileDto renameFileDto) {

        fileStorageService.renameFile(filename, renameFileDto.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileDto>> getFileList(
            @RequestParam(value = "limit", required = false) Integer limit) {

        List<FileDto> files = fileStorageService.getFilesList(limit);
        return ResponseEntity.ok(files);
    }
}
