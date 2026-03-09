package org.forecast.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class CompanyLogoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif",
            // SVG is commonly sent as image/svg+xml
            "image/svg+xml",
            // keep for backwards/quirky clients
            "image/svg"
    );

    /**
     * Root uploads directory on disk.
     */
    private final Path uploadsRoot;

    public CompanyLogoStorageService(@Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.uploadsRoot = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    /**
     * Stores the uploaded logo to disk and returns the public URL path (under /uploads/**).
     */
    public String storeCompanyLogo(UUID companyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported logo content type: " + contentType);
        }

        String ext = extensionFor(contentType);

        try {
            Path companyDir = uploadsRoot.resolve("company-logos");
            Files.createDirectories(companyDir);

            Path target = companyDir.resolve(companyId.toString() + "." + ext).normalize();
            if (!target.startsWith(companyDir)) {
                throw new IllegalStateException("Invalid logo path");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/company-logos/" + target.getFileName();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store company logo", e);
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml", "image/svg" -> "svg";
            default -> "bin";
        };
    }
}
