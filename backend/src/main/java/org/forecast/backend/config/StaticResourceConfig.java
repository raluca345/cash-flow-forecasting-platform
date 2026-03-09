package org.forecast.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves user-uploaded files from a local folder.
 *
 * <p>Maps HTTP URLs under {@code /uploads/**} to a directory on disk.
 * This is handy for local dev and simple deployments.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    /**
     * Root directory on disk that contains uploaded files.
     *
     * <p>Default: {@code uploads}
     */
    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsPath = Path.of(uploadsDir).toAbsolutePath().normalize();
        String location = uploadsPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}

