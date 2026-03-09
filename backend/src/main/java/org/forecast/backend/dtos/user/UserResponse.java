package org.forecast.backend.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.enums.Role;
import org.forecast.backend.model.User;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private Role role;
    private UUID companyId;
    private String profilePictureUrl;

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .companyId(user.getCompany() == null ? null : user.getCompany().getId())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    public static List<UserResponse> fromEntities(List<User> users) {
        return users == null ? List.of() : users.stream().map(UserResponse::fromEntity).toList();
    }
}

