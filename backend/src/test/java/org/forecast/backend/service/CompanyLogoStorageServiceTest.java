package org.forecast.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyLogoStorageServiceTest {

    @Test
    void storeCompanyLogo_rejectsSvgContentType() throws Exception {
        Path tempDir = Files.createTempDirectory("uploads-test-");
        CompanyLogoStorageService service = new CompanyLogoStorageService(tempDir.toString());

        UUID companyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes()
        );

        assertThatThrownBy(() -> service.storeCompanyLogo(companyId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported logo content type");
    }

    @Test
    void storeCompanyLogo_rejectsUnknownContentType() {
        CompanyLogoStorageService service = new CompanyLogoStorageService("build/uploads-test");

        UUID companyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.txt",
                "text/plain",
                "nope".getBytes()
        );

        assertThatThrownBy(() -> service.storeCompanyLogo(companyId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported logo content type");
    }
}

