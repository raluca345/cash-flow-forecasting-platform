package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.shared.PaginatedResponse;
import org.forecast.backend.dtos.user.CreateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRequest;
import org.forecast.backend.dtos.user.UpdateUserRoleRequest;
import org.forecast.backend.dtos.user.UserResponse;
import org.forecast.backend.model.User;
import org.forecast.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<UserResponse>> listUsers(
            @PageableDefault(size = 10, page = 0, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<User> userPage = userService.listAll(pageable);
        PaginatedResponse<UserResponse> response = PaginatedResponse.fromPageContent(
                userPage,
                UserResponse.fromEntities(userPage.getContent())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.create(request);
        return ResponseEntity.ok(UserResponse.fromEntity(created));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        User updated = userService.update(userId, request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PostMapping(value = "/{userId}/profile-picture", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<UserResponse> setProfilePictureUrl(
            @PathVariable UUID userId,
            @RequestBody String profilePictureUrl
    ) {
        User updated = userService.updateProfilePictureUrl(userId, profilePictureUrl);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        User updated = userService.updateRole(userId, request.getRole());
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }
}
