package com.nso.web.controller.system;

import com.nso.common.core.domain.AjaxResult;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.service.ISysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/system/users", "/api/v1/admin/system/user"})
@PreAuthorize("hasRole('ADMIN')")
public class SysUserController {
    private final ISysUserService users;
    private final PasswordEncoder passwordEncoder;

    public SysUserController(ISysUserService users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public AjaxResult<?> list() {
        List<Map<String, Object>> rows = users.listUsers().stream().map(user -> Map.<String, Object>of(
                "id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname(),
                "deptId", user.getDeptId() == null ? 0L : user.getDeptId(), "status", user.getStatus(),
                "roles", users.roleCodes(user.getId()))).toList();
        return AjaxResult.success(rows);
    }

    @PostMapping
    public AjaxResult<?> create(@RequestBody CreateUserRequest request) {
        if (request == null || request.password() == null || request.password().length() < 6) {
            throw new BusinessException("初始密码至少需要 6 位");
        }
        SysUser user = users.createUser(request.username(), passwordEncoder.encode(request.password()), request.nickname(), request.deptId(), request.roleCodes());
        return AjaxResult.success(Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname(), "status", user.getStatus()));
    }

    @PutMapping("/{userId}/roles")
    public AjaxResult<?> replaceRoles(@PathVariable Long userId, @RequestBody RoleCodesRequest request) {
        users.replaceUserRoles(userId, request == null ? List.of() : request.roleCodes());
        return AjaxResult.success();
    }

    public record CreateUserRequest(String username, String password, String nickname, Long deptId, List<String> roleCodes) { }
    public record RoleCodesRequest(List<String> roleCodes) { }
}
