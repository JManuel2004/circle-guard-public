package com.circleguard.file.controller;

import com.circleguard.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileStorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String filename = storageService.saveFile(file);
        return ResponseEntity.ok(Map.of("filename", filename));
    }
}
