package com.school.ticket.controller;

import com.school.ticket.service.FileStorageService;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * 图片上传与访问(公开:报修属于公开操作)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    /** 上传报修图片,返回文件名与访问地址 */
    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String name = storage.store(file);
        return Map.of("name", name, "url", "/api/files/" + name);
    }

    /** 读取图片 */
    @GetMapping("/files/{name}")
    public ResponseEntity<Resource> get(@PathVariable String name) {
        Path p = storage.load(name);
        Resource res = new FileSystemResource(p);
        MediaType mediaType = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(res);
    }
}
