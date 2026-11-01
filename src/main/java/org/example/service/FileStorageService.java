package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
            "jpg",  new byte[][] {{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }},
            "jpeg", new byte[][] {{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }},
            "png",  new byte[][] {{ (byte) 0x89, 0x50, 0x4E, 0x47 }},
            "gif",  new byte[][] {{ 0x47, 0x49, 0x46, 0x38 }},
            "webp", new byte[][] {{ 0x52, 0x49, 0x46, 0x46 }}
    );

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Upload qovluğu yaradıla bilmədi: " + this.root, e);
        }
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fayl boşdur");
        }
        String original = file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Yalnız şəkil faylları: jpg, png, gif, webp");
        }
        validateMagicBytes(file, ext);

        String filename = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Yanlış fayl yolu");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + filename;
    }

    private void validateMagicBytes(MultipartFile file, String ext) throws IOException {
        byte[][] signatures = MAGIC_BYTES.get(ext);
        if (signatures == null) {
            throw new IllegalArgumentException("Dəstəklənməyən fayl tipi");
        }
        byte[] header = file.getInputStream().readNBytes(12);
        if (header.length < 3) {
            throw new IllegalArgumentException("Fayl formatı etibarsızdır");
        }
        boolean match = false;
        for (byte[] sig : signatures) {
            if (header.length >= sig.length) {
                boolean ok = true;
                for (int i = 0; i < sig.length; i++) {
                    if (header[i] != sig[i]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            throw new IllegalArgumentException("Fayl məzmunu uzantı ilə uyğun gəlmir");
        }
    }

    private static String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
