package com.school.ticket.service;

import com.school.ticket.config.AppProperties;
import com.school.ticket.web.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/** 报修图片存储 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties props;
    private Path root;

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + e.getMessage(), e);
        }
    }

    /** 保存图片,返回随机文件名 */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(400, "文件为空");
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) throw new ApiException(400, "只支持 JPG / PNG / WEBP / GIF 图片");
        if (file.getSize() > 8 * 1024 * 1024) throw new ApiException(400, "图片不能超过 8MB");
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path target = root.resolve(name).normalize();
            if (!target.getParent().equals(root)) throw new ApiException(400, "非法文件名");
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(500, "保存失败: " + e.getMessage());
        }
        return name;
    }

    /** 判断文件是否真实存在(且文件名合法) */
    public boolean exists(String name) {
        if (name == null || !name.matches("[A-Za-z0-9._-]{1,80}")) return false;
        Path p = root.resolve(name).normalize();
        return p.getParent().equals(root) && Files.exists(p);
    }

    /** 读取文件(防目录穿越) */
    public Path load(String name) {
        if (name == null || !name.matches("[A-Za-z0-9._-]{1,80}")) {
            throw new ApiException(400, "非法文件名");
        }
        Path p = root.resolve(name).normalize();
        if (!p.getParent().equals(root) || !Files.exists(p)) {
            throw new ApiException(404, "文件不存在");
        }
        return p;
    }
}
