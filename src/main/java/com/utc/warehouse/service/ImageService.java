package com.utc.warehouse.service;

import com.utc.warehouse.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Quản lý file ảnh sản phẩm.
 *  - Lưu trong thư mục <user.home>/.warehouse_management/product_images/
 *  - Tự resize về tối đa MAX_DIMENSION để tiết kiệm dung lượng
 *  - Hỗ trợ JPEG, PNG, GIF, WEBP, BMP
 *  - Giới hạn kích thước file tối đa 5 MB
 *
 * <p>Đường dẫn lưu trong DB là dạng tương đối (vd "abc-123.jpg") — khi cần dùng, resolve qua {@link #resolve(String)}.
 */
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    public static final int MAX_DIMENSION = 800;
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
    public static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final Path baseDir;

    public ImageService() {
        this.baseDir = AppPaths.getProductImageDir();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            log.error("Cannot create image dir {}", baseDir, ex);
        }
    }

    public String save(File source) throws IOException {
        if (source == null || !source.isFile() || !source.canRead())
            throw new IllegalArgumentException("File không hợp lệ: " + source);
        if (source.length() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("File quá lớn (> " + (MAX_FILE_SIZE / 1024 / 1024) + " MB)");

        String ext = getExt(source.getName());
        if (!ALLOWED_EXT.contains(ext.toLowerCase()))
            throw new IllegalArgumentException("Định dạng không hỗ trợ: ." + ext);

        BufferedImage src;
        try (InputStream in = Files.newInputStream(source.toPath())) {
            src = ImageIO.read(in);
        }
        if (src == null) throw new IllegalArgumentException("File không phải ảnh hợp lệ");

        boolean hasAlpha = src.getColorModel().hasAlpha();
        String outExt = hasAlpha ? "png" : "jpg";
        String filename = UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "." + outExt;
        Path target = baseDir.resolve(filename);

        BufferedImage resized = resizeIfNeeded(src, MAX_DIMENSION);
        boolean ok = ImageIO.write(resized, outExt, target.toFile());
        if (!ok) throw new IOException("Không ghi được ảnh ra " + target);

        log.info("Saved product image: {} ({}x{}, {} bytes)",
                filename, resized.getWidth(), resized.getHeight(), target.toFile().length());
        return filename;
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Refusing to delete suspicious path: {}", filename);
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(filename));
        } catch (IOException ex) {
            log.warn("Cannot delete image {}: {}", filename, ex.getMessage());
        }
    }

    public Path resolve(String filename) {
        if (filename == null || filename.isBlank()) return null;
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) return null;
        Path p = baseDir.resolve(filename);
        return Files.exists(p) ? p : null;
    }

    public BufferedImage load(String filename) {
        Path p = resolve(filename);
        if (p == null) return null;
        try {
            return ImageIO.read(p.toFile());
        } catch (IOException ex) {
            log.warn("Cannot read image {}: {}", filename, ex.getMessage());
            return null;
        }
    }

    public Path getBaseDir() { return baseDir; }

    private static String getExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    public static BufferedImage resizeIfNeeded(BufferedImage src, int maxDim) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxDim && h <= maxDim) return src;
        double scale = Math.min((double) maxDim / w, (double) maxDim / h);
        int nw = (int) Math.round(w * scale);
        int nh = (int) Math.round(h * scale);
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= nw && cur.getHeight() / 2 >= nh) {
            BufferedImage tmp = new BufferedImage(cur.getWidth() / 2, cur.getHeight() / 2, cur.getType());
            Graphics2D g = tmp.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(cur, 0, 0, tmp.getWidth(), tmp.getHeight(), null);
            g.dispose();
            cur = tmp;
        }
        BufferedImage out = new BufferedImage(nw, nh, cur.getType());
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(cur, 0, 0, nw, nh, null);
        g2.dispose();
        return out;
    }
}
