package com.school.ticket.service;

import com.school.ticket.config.AppProperties;
import com.school.ticket.web.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/** 报修图片存储:上传即压缩(缩放 + JPEG 重编码),并支持按天清理 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final AppProperties props;
    private Path root;

    /** 压缩参数:最长边上限与 JPEG 质量 */
    private static final int MAX_DIM = 1600;
    private static final float JPEG_QUALITY = 0.8f;

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

    /** 保存图片(尽量压缩为 JPEG),返回随机文件名 */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(400, "文件为空");
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) throw new ApiException(400, "只支持 JPG / PNG / WEBP / GIF 图片");
        if (file.getSize() > 8 * 1024 * 1024) throw new ApiException(400, "图片不能超过 8MB");

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(500, "读取文件失败: " + e.getMessage());
        }

        String base = UUID.randomUUID().toString().replace("-", "");
        // GIF 可能是动图,直接原样存,避免只留首帧;其余尝试压缩为 JPEG
        boolean tryCompress = !"image/gif".equals(file.getContentType());
        BufferedImage img = null;
        if (tryCompress) {
            try { img = ImageIO.read(new ByteArrayInputStream(bytes)); } catch (IOException ignored) { /* 解码失败则原样存 */ }
        }

        try {
            if (img != null) {
                String name = base + ".jpg";
                Path target = safeTarget(name);
                BufferedImage rgb = prepareRgb(img);
                writeJpeg(rgb, target, JPEG_QUALITY);
                return name;
            } else {
                // 无法压缩:原样保存
                String name = base + ext;
                Path target = safeTarget(name);
                Files.write(target, bytes);
                return name;
            }
        } catch (IOException e) {
            throw new ApiException(500, "保存失败: " + e.getMessage());
        }
    }

    private Path safeTarget(String name) {
        Path target = root.resolve(name).normalize();
        if (!target.getParent().equals(root)) throw new ApiException(400, "非法文件名");
        return target;
    }

    /** 缩放到最长边不超过 MAX_DIM,并铺白底转为不含透明通道的 RGB(适合 JPEG) */
    private BufferedImage prepareRgb(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int max = Math.max(w, h);
        int nw = w, nh = h;
        if (max > MAX_DIM) {
            double r = (double) MAX_DIM / max;
            nw = Math.max(1, (int) Math.round(w * r));
            nh = Math.max(1, (int) Math.round(h * r));
        }
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private void writeJpeg(BufferedImage img, Path target, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (OutputStream os = Files.newOutputStream(target);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
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

    /**
     * 删除最后修改时间早于 days 天的图片文件。days<=0 表示不清理。
     * @return 删除的文件数
     */
    public int cleanupOlderThan(int days) {
        if (days <= 0) return 0;
        long cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
        int[] count = {0};
        try (Stream<Path> s = Files.list(root)) {
            s.filter(Files::isRegularFile).forEach(p -> {
                try {
                    FileTime t = Files.getLastModifiedTime(p);
                    if (t.toMillis() < cutoff) {
                        Files.deleteIfExists(p);
                        count[0]++;
                    }
                } catch (IOException e) {
                    log.warn("删除图片失败 {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("扫描上传目录失败: {}", e.getMessage());
        }
        return count[0];
    }
}
