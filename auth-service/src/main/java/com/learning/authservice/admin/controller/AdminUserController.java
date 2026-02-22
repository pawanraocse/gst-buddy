package com.learning.authservice.admin.controller;

import com.learning.authservice.admin.dto.AdminUserDetailDto;
import com.learning.authservice.admin.dto.AssignRoleRequest;
import com.learning.authservice.admin.service.AdminUserService;
import com.learning.common.constants.HeaderNames;
import com.learning.common.infra.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Platform admin endpoints for managing all users (cross-tenant).
 * Every method requires a specific permission enforced by {@code @RequirePermission}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @RequirePermission(resource = "user", action = "manage")
    public ResponseEntity<Page<AdminUserDetailDto>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminUserService.listAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    @RequirePermission(resource = "user", action = "manage")
    public ResponseEntity<AdminUserDetailDto> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    @PostMapping("/{userId}/enable")
    @RequirePermission(resource = "account", action = "suspend")
    public ResponseEntity<Void> enableUser(@PathVariable String userId) {
        adminUserService.enableUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/suspend")
    @RequirePermission(resource = "account", action = "suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable String userId) {
        adminUserService.suspendUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @RequirePermission(resource = "account", action = "delete")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles")
    @RequirePermission(resource = "user", action = "manage")
    public ResponseEntity<Void> assignRole(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request,
            HttpServletRequest httpRequest) {

        String adminId = httpRequest.getHeader(HeaderNames.USER_ID);
        adminUserService.assignRole(userId, request.roleId(), adminId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @RequirePermission(resource = "user", action = "manage")
    public ResponseEntity<Void> removeRole(
            @PathVariable String userId,
            @PathVariable String roleId) {
        adminUserService.removeRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
