package com.srm.erp.security;

import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {
    private final AdminUserService users;

    public AdminUserController(AdminUserService users) {
        this.users = users;
    }

    @GetMapping
    public ApiResponse<List<AdminUserService.AdminUser>> list() {
        return ApiResponse.success("Users retrieved successfully", users.findAll());
    }

    @PostMapping
    public ApiResponse<AdminUserService.AdminUser> create(@Valid @RequestBody CreateUserRequest request) {
        UUID actorId = currentActor();
        return ApiResponse.success("User created successfully", users.create(request, actorId));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<AdminUserService.AdminUser> updateStatus(
            @PathVariable UUID userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success("User account status updated successfully",
                users.updateStatus(userId, request.accountStatus(), currentActor()));
    }

    private UUID currentActor() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public record CreateUserRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String fullName,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
            @jakarta.validation.constraints.Size(max = 254) String email,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 12, max = 128)
            String password,
            @jakarta.validation.constraints.NotEmpty List<@jakarta.validation.constraints.Pattern(
                    regexp = "^(SUPER_ADMIN|SRM_ADMIN|SUPPLIER_VERIFIER|PROCUREMENT_BUYER|PROCUREMENT_APPROVER|AUDITOR)$")
                    String> roles) {}

    public record UpdateUserStatusRequest(
            @jakarta.validation.constraints.Pattern(regexp = "^(ACTIVE|SUSPENDED|DEACTIVATED)$")
            String accountStatus) {}
}
