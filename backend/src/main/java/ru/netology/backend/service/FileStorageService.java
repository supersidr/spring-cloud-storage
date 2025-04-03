package ru.netology.backend.service;

import ru.netology.backend.model.dto.FileDto;
import ru.netology.backend.model.entity.FileEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    FileEntity storeFile(MultipartFile file, String filename);
    Resource loadFileAsResource(String filename);
    List<FileDto> getFilesList(Integer limit);
    void deleteFile(String filename);
    void renameFile(String oldFilename, String newFilename);
}