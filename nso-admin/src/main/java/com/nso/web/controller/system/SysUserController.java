package com.nso.web.controller.system;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.TenantContext;
import com.nso.shared.core.domain.AjaxResult;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.service.IPersonnelOrganizationService;
import com.nso.system.service.ISysDeptService;
import com.nso.system.service.ISysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 用户管理接口。
@RestController
@RequestMapping({"/api/v1/admin/system/users", "/api/v1/admin/system/user"})
public class SysUserController {
    // 系统用户服务
    private final ISysUserService users;
    // 系统部门服务
    private final ISysDeptService departments;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;
    // 人员组织服务
    private final IPersonnelOrganizationService personnel;

    public SysUserController(ISysUserService users,
                             ISysDeptService departments,
                             PasswordEncoder passwordEncoder,
                             IPersonnelOrganizationService personnel) {
        this.users = users;
        this.departments = departments;
        this.passwordEncoder = passwordEncoder;
        this.personnel = personnel;
    }

    // 分页查询用户。
    @GetMapping
    @PreAuthorize("hasAuthority('sys:user:read')")
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        Map<Long, String> deptNames = departments.list().stream()
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getDeptName(), (left, right) -> left));
        PageResult<SysUser> userPage = users.listUsers(new PageQuery(pageNo, pageSize));
        List<Map<String, Object>> rows = userPage.list().stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            row.put("deptId", user.getDeptId());
            row.put("deptName", user.getDeptId() == null ? null : deptNames.get(user.getDeptId()));
            row.put("status", user.getStatus());
            row.put("userType", user.getUserType());
            row.put("roles", users.roleCodes(user.getId()));
            row.put("employeeNo", user.getEmployeeNo());
            row.put("forceChangePassword", Integer.valueOf(1).equals(user.getForceChangePassword()));
            row.put("postIds", personnel.userPostIds(user.getId()));
            return row;
        }).toList();
        PageQuery page = new PageQuery(pageNo, pageSize);
        return AjaxResult.success(new PageResult<>(rows, userPage.total(), page.pageNoValue(), page.pageSizeValue()));
    }

    // 创建用户。
    @PostMapping
    @PreAuthorize("hasAuthority('sys:user:create')")
    public AjaxResult<?> create(@RequestBody CreateUserRequest request) {
        if (request == null || request.password() == null || request.password().length() < 6) {
            throw new BusinessException("初始密码至少需要 6 位");
        }
        if (isHrOnly() && request.roleCodes() != null && !request.roleCodes().isEmpty()) {
            throw new BusinessException("人事管理员只能建档，系统角色须由系统管理员另行授权");
        }
        SysUser user = users.createUser(request.username(), passwordEncoder.encode(request.password()),
                request.nickname(), request.deptId(), request.roleCodes());
        if (request.employeeNo() != null && !request.employeeNo().isBlank()) {
            users.assignEmployeeNo(user.getId(), request.employeeNo());
        }
        return AjaxResult.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "status", user.getStatus()));
    }

    // 替换用户角色。
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('sys:role:grant')")
    public AjaxResult<?> replaceRoles(@PathVariable Long userId, @RequestBody RoleCodesRequest request) {
        users.replaceUserRoles(userId, request == null ? List.of() : request.roleCodes());
        return AjaxResult.success();
    }

    // 更新用户基础信息。
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public AjaxResult<?> update(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("用户参数不能为空");
        }
        if (isHrOnly() && request.roleCodes() != null) {
            throw new BusinessException("人事管理员不能授予或调整系统角色");
        }
        SysUser user = users.updateUser(userId, request.nickname(), request.deptId(), request.roleCodes(), request.status());
        return AjaxResult.success(Map.of("id", user.getId(), "username", user.getUsername(), "status", user.getStatus()));
    }

    // 重置用户密码。
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('sys:user:reset-password')")
    public AjaxResult<?> resetPassword(@PathVariable Long userId, @RequestBody PasswordResetRequest request) {
        if (request == null || request.password() == null || request.password().length() < 8) {
            throw new BusinessException("重置密码至少需要 8 位");
        }
        users.resetPassword(userId, passwordEncoder.encode(request.password()));
        return AjaxResult.success();
    }

    // 激活用户账号。
    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAuthority('sys:user:lifecycle')")
    public AjaxResult<?> activate(@PathVariable Long userId, @RequestBody ActivationRequest request) {
        if (request == null || request.password() == null || request.password().length() < 8) {
            throw new BusinessException("临时密码至少需要 8 位");
        }
        SysUser user = users.activateUser(userId, passwordEncoder.encode(request.password()),
                null, request.reason());
        return AjaxResult.success(Map.of(
                "id", user.getId(),
                "status", user.getStatus(),
                "forceChangePassword", true));
    }

    // 检查用户离岗交接条件。
    @GetMapping("/{userId}/handover-check")
    @PreAuthorize("hasAuthority('sys:user:lifecycle')")
    public AjaxResult<?> handoverCheck(@PathVariable Long userId) {
        return AjaxResult.success(users.handoverCheck(userId));
    }

    // 完成用户生命周期状态变更。
    @PutMapping("/{userId}/lifecycle")
    @PreAuthorize("hasAuthority('sys:user:lifecycle')")
    public AjaxResult<?> completeLifecycle(@PathVariable Long userId, @RequestBody LifecycleRequest request) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new BusinessException("生命周期状态不能为空");
        }
        SysUser user = users.completeLifecycle(userId, request.status(), request.reason());
        return AjaxResult.success(Map.of("id", user.getId(), "status", user.getStatus()));
    }

    // 替换用户岗位。
    @PutMapping("/{userId}/posts")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public AjaxResult<?> replacePosts(@PathVariable Long userId, @RequestBody PostAssignmentRequest request) {
        personnel.replaceUserPosts(userId,
                request == null ? List.of() : request.postIds(),
                request == null ? null : request.primaryPostId());
        return AjaxResult.success();
    }

    private boolean isHrOnly() {
        return TenantContext.hasAnyRole("hr_admin")
                && !TenantContext.hasAnyRole("superadmin", "system_admin", "admin");
    }

    public record CreateUserRequest(
        // 用户名
        String username,
        // 初始密码
        String password,
        // 用户昵称
        String nickname,
        // 部门编号
        Long deptId,
        // 角色编码列表
        List<String> roleCodes,
        // 工号
        String employeeNo
    ) {
    }

    public record RoleCodesRequest(
        // 角色编码列表
        List<String> roleCodes
    ) {
    }

    public record UserUpdateRequest(
        // 用户昵称
        String nickname,
        // 部门编号
        Long deptId,
        // 角色编码列表
        List<String> roleCodes,
        // 用户状态
        String status
    ) {
    }

    public record PasswordResetRequest(
        // 新密码
        String password
    ) {
    }

    public record ActivationRequest(
        // 临时密码
        String password,
        // 激活原因
        String reason
    ) {
    }

    public record LifecycleRequest(
        // 生命周期状态
        String status,
        // 变更原因
        String reason
    ) {
    }

    public record PostAssignmentRequest(
        // 岗位编号列表
        List<Long> postIds,
        // 主岗位编号
        Long primaryPostId
    ) {
    }
}
