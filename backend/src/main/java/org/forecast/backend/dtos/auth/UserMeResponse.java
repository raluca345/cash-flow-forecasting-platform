package org.forecast.backend.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.model.User;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String profilePictureUrl;

    private CompanySummary company;

    public static UserMeResponse fromEntity(User user) {
        if (user == null) return null;
        return UserMeResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .company(user.getCompany() == null ? null : CompanySummary.builder()
                        .id(user.getCompany().getId())
                        .name(user.getCompany().getName())
                        .logoUrl(user.getCompany().getLogoUrl())
                        .build())
                .build();
    }

    @Data
    @Builder
    public static class CompanySummary {
        private UUID id;
        private String name;
        private String logoUrl;
    }
}
