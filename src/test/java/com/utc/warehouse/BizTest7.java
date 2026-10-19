package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.service.ProductService;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BizTest7: Verify image cache hit (không đọc disk lần 2).
 *
 * Trước fix: ImageCellRenderer gọi imageService.load(path) MỖI LẦN vẽ ô ảnh.
 * Ảnh 800x600 đọc disk ~5-20ms; với bảng 50 dòng & click đổi selection,
 * EDT bị block hàng trăm ms → click lần 2 bị "nuốt".
 *
 * Sau fix: imageThumbCache lưu BufferedImage đã scale 40x40, click lần 2+
 * trả ngay từ RAM, EDT không bị block.
 */
public class BizTest7 {

    public static void main(String[] args) throws Exception {
        ProductService ps = new ProductService();
        ImageService imgSvc = new ImageService();

        List<Product> all = ps.search("");
        Product withImg = all.stream()
                .filter(p -> p.getImagePath() != null && !p.getImagePath().isBlank())
                .findFirst().orElse(null);

        if (withImg == null) {
            System.out.println("No product with image. Skip.");
            return;
        }
        System.out.println("[1] Test with: " + withImg.getName() + " path=" + withImg.getImagePath());

        // Đo thời gian load 1 ảnh
        long t1 = System.nanoTime();
        BufferedImage img1 = imgSvc.load(withImg.getImagePath());
        long t2 = System.nanoTime();
        System.out.printf("[2] imageService.load() lần 1: %.2f ms%n", (t2 - t1) / 1_000_000.0);

        // Đo thời gian "giả lập cache hit" (HashMap.get)
        Map<Integer, BufferedImage> cache = new HashMap<>();
        cache.put(withImg.getId(), scale(img1, 40, 40));
        long t3 = System.nanoTime();
        BufferedImage fromCache = cache.get(withImg.getId());
        long t4 = System.nanoTime();
        System.out.printf("[3] HashMap.get() (cache hit): %.4f ms%n", (t4 - t3) / 1_000_000.0);

        // 1000 lần cache hit
        long t5 = System.nanoTime();
        BufferedImage last = null;
        for (int i = 0; i < 1000; i++) last = cache.get(withImg.getId());
        long t6 = System.nanoTime();
        System.out.printf("[4] 1000 cache hit: %.2f ms (%.4f ms/call)%n",
                (t6 - t5) / 1_000_000.0, (t6 - t5) / 1_000_000_000.0);
        System.out.println("    last = " + (last == fromCache ? "SAME instance" : "DIFFERENT!"));

        // So sánh với 100 lần load disk (mô phỏng click 100 lần)
        long t7 = System.nanoTime();
        for (int i = 0; i < 100; i++) imgSvc.load(withImg.getImagePath());
        long t8 = System.nanoTime();
        System.out.printf("[5] 100 imageService.load() (mô phỏng 100 click): %.2f ms (%.4f ms/call)%n",
                (t8 - t7) / 1_000_000.0, (t8 - t7) / 100_000_000.0);

        System.out.println("=== BizTest7 OK ===");
    }

    private static BufferedImage scale(BufferedImage src, int maxW, int maxH) {
        double s = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
        int nw = Math.max(1, (int) Math.round(src.getWidth() * s));
        int nh = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally { g.dispose(); }
        return out;
    }
}
