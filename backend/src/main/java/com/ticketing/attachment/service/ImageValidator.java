package com.ticketing.attachment.service;

import com.ticketing.common.api.BusinessException;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator {
    public record ImageInfo(String mime, String extension, int width, int height) { }
    public ImageInfo validate(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024) throw BusinessException.invalid("图片最大5 MiB且不能为空");
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw BusinessException.invalid("无法识别图片格式");
            var reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String mime = switch (format) {
                    case "png" -> "image/png"; case "jpeg", "jpg" -> "image/jpeg";
                    case "webp" -> "image/webp"; default -> throw BusinessException.invalid("仅支持PNG、JPEG、WebP");
                };
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 20_000_000)
                    throw BusinessException.invalid("图片解码尺寸不能超过2000万像素");
                if (reader.read(0) == null) throw BusinessException.invalid("图片损坏");
                return new ImageInfo(mime, format.equals("jpeg") ? "jpg" : format, width, height);
            } finally { reader.dispose(); }
        } catch (BusinessException ex) { throw ex; }
        catch (Exception ex) { throw BusinessException.invalid("图片无法解码或已经损坏"); }
    }
}
