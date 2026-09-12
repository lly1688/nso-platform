package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysRoleMapper;
import com.nso.system.mapper.SysMenuMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.mapper.SysUserRoleMapper;
import com.nso.system.domain.SysDept;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.security.UserLifecycle;
import com.nso.system.service.ISysUserService;
import com.nso.shared.exception.BusinessException;
import com.nso.business.core.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service

// 用户管理 服务层处理
public class SysUserServiceImpl implements ISysUserService {

    // 系统用户数据映射
    private final SysUserMapper userMapper;
    // 系统角色数据映射
    private final SysRoleMapper roleMapper;
    // 系统菜单数据映射
    private final SysMenuMapper menuMapper;
    // 系统用户角色数据映射
    private final SysUserRoleMapper userRoleMapper;
    // 系统部门数据映射
    private final SysDeptMapper deptMapper;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public SysUserServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper, SysUserRoleMapper userRoleMapper, SysDeptMapper deptMapper, JdbcTemplate jdbc) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.deptMapper = deptMapper;
        this.jdbc = jdbc;
    }

    // 按用户名查询用户。
    @Override
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getUserType, "INTERNAL")
                .eq(SysUser::getStatus, UserLifecycle.ACTIVE)));
    }

    // 按编号查询用户。
    @Override
    public Optional<SysUser> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    // 查询用户角色标识。
    @Override
    public List<String> roleCodes(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    // 查询用户权限标识。
    @Override
    public List<String> permissionCodes(Long userId) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        return menuMapper.selectPermissionCodesByUserId(userId, user.getTenantId());
    }

    // 查询用户菜单路由。
    @Override
    public List<String> menuRoutes(Long userId) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        return menuMapper.selectMenuRoutesByUserId(userId, user.getTenantId());
    }

    // 查询用户授权版本。
    @Override
    public int authVersion(Long userId) {
        return findById(userId).map(user -> user.getAuthVersion() == null ? 0 : user.getAuthVersion()).orElse(-1);
    }

    // 查询用户列表。
    @Override
    public List<SysUser> listUsers() {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getTenantId, TenantContext.tenantId()).orderByAsc(SysUser::getId));
    }

    // 分页查询用户列表。
    @Override
    public PageResult<SysUser> listUsers(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SysUser> entityPage = userMapper.selectPage(PageSupport.page(page), Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getTenantId, TenantContext.tenantId()).orderByAsc(SysUser::getId));
        return new PageResult<>(entityPage.getRecords(), entityPage.getTotal(), page.pageNoValue(), page.pageSizeValue());
    }

    // 创建系统用户。
    @Override
    @Transactional
    public SysUser createUser(String username, String passwordHash, String nickname, Long deptId, List<String> roleCodes) {
        if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        if (userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username.trim())) != null) {
            throw new BusinessException("用户名已存在");
        }
        List<String> assignedRoles = roleCodes == null ? List.of() : roleCodes;
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setTenantId(TenantContext.tenantId());
        verifyDepartment(deptId);
        user.setDeptId(deptId);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname == null || nickname.isBlank() ? username.trim() : nickname.trim());
        user.setStatus(UserLifecycle.PENDING);
        user.setForceChangePassword(1);
        user.setUserType("INTERNAL");
        userMapper.insert(user);
        for (String roleCode : assignedRoles) {
            Long roleId = roleMapper.selectIdByRoleCode(user.getTenantId(), roleCode.trim().toLowerCase());
            if (roleId == null) throw new BusinessException("角色不存在：" + roleCode);
            userRoleMapper.insertRole(user.getId(), roleId);
        }
        return user;
    }

    // 更新系统用户。
    @Override
    @Transactional
    public SysUser updateUser(Long userId, String nickname, Long deptId, List<String> roleCodes, String status) {
        SysUser user = requireCurrentTenant(userId);
        if (!"INTERNAL".equals(user.getUserType()) && deptId != null) throw new BusinessException("外部客户账号不能归属内部部门");
        if (!"INTERNAL".equals(user.getUserType()) && roleCodes != null && roleCodes.stream().anyMatch(role -> !"customer_confirm".equalsIgnoreCase(role))) {
            throw new BusinessException("外部客户账号只能保留 customer_confirm 角色");
        }
        verifyDepartment(deptId);
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname.trim());
        user.setDeptId(deptId);
        if (status != null && !status.isBlank()) {
            String target = UserLifecycle.normalize(status);
            if (!target.equals(user.getStatus())) throw new BusinessException("账号状态变更必须通过交接与生命周期接口执行");
        }
        userMapper.updateById(user);
        if (roleCodes != null) replaceUserRoles(userId, roleCodes);
        userMapper.incrementAuthVersion(userId);
        return userMapper.selectById(userId);
    }

    // 激活用户并设置初始权限。
    @Override
    @Transactional
    public SysUser activateUser(Long userId, String passwordHash, List<String> roleCodes, String reason) {
        SysUser user = requireCurrentTenant(userId);
        if (!"INTERNAL".equals(user.getUserType())) throw new BusinessException("外部身份不能激活为内部账号");
        if (passwordHash == null || passwordHash.isBlank()) throw new BusinessException("激活账号必须设置临时密码或激活密钥");
        String before = user.getStatus();
        user.setPasswordHash(passwordHash);
        user.setForceChangePassword(1);
        user.setStatus(UserLifecycle.ACTIVE);
        user.setStatusReason(reason == null || reason.isBlank() ? "账号激活" : reason.trim());
        userMapper.updateById(user);
        if (roleCodes != null) replaceUserRoles(userId, roleCodes);
        userMapper.incrementAuthVersion(userId);
        audit(userId, "ACCOUNT_ACTIVATED", before, UserLifecycle.ACTIVE, user.getStatusReason());
        return userMapper.selectById(userId);
    }

    // 检查用户离岗前的待交接业务。
    @Override
    public Map<String, Integer> handoverCheck(Long userId) {
        requireCurrentTenant(userId);
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("projectManagers", count("SELECT COUNT(*) FROM nso_project WHERE tenant_id=? AND owner_user_id=? AND deleted=0 AND status NOT IN ('COMPLETED','CANCELLED','ARCHIVED')", userId));
        result.put("openTasks", count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=? AND assignee_id=? AND status NOT IN ('DONE','CANCELLED')", userId));
        result.put("riskActions", count("SELECT COUNT(*) FROM nso_risk_action WHERE tenant_id=? AND responsible_user_id=? AND status<>'CLOSED' AND deleted=0", userId));
        result.put("pendingApprovals", count("SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=? AND approver_id=? AND decision='PENDING'", userId));
        result.put("departmentLeadership", count("SELECT COUNT(*) FROM sys_dept WHERE tenant_id=? AND leader_user_id=? AND deleted=0", userId));
        result.put("unreadMessages", count("SELECT COUNT(*) FROM nso_message WHERE tenant_id=? AND receiver_id=? AND status='UNREAD' AND deleted=0", userId));
        return result;
    }

    // 完成用户生命周期状态变更。
    @Override
    @Transactional
    public SysUser completeLifecycle(Long userId, String status, String reason) {
        SysUser user = requireCurrentTenant(userId);
        String target = UserLifecycle.normalize(status);
        if (Set.of(UserLifecycle.DISABLED, UserLifecycle.LEFT).contains(target)) {
            Map<String, Integer> open = handoverCheck(userId);
            if (open.values().stream().anyMatch(value -> value != null && value > 0)) {
                throw new BusinessException("账号仍有项目负责人、任务、审批、风险、部门负责人或未读待办，必须完成交接后才能停用或离职");
            }
        }
        String before = user.getStatus();
        user.setStatus(target);
        user.setStatusReason(reason == null || reason.isBlank() ? "账号生命周期变更" : reason.trim());
        userMapper.updateById(user);
        userMapper.incrementAuthVersion(userId);
        audit(userId, "ACCOUNT_LIFECYCLE_" + target, before, target, user.getStatusReason());
        return userMapper.selectById(userId);
    }

    // 分配员工编号。
    @Override
    @Transactional
    public void assignEmployeeNo(Long userId, String employeeNo) {
        SysUser user = requireCurrentTenant(userId);
        if (!"INTERNAL".equals(user.getUserType())) throw new BusinessException("员工编号只适用于内部账号");
        String normalized = employeeNo == null ? null : employeeNo.trim();
        if (normalized == null || normalized.isBlank()) throw new BusinessException("员工编号不能为空");
        long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getTenantId, TenantContext.tenantId())
                .eq(SysUser::getEmployeeNo, normalized).ne(SysUser::getId, userId));
        if (exists > 0) throw new BusinessException("员工编号已存在");
        user.setEmployeeNo(normalized);
        userMapper.updateById(user);
        audit(userId, "EMPLOYEE_NO_ASSIGNED", null, normalized, "人员档案更新");
    }

    // 重置用户密码。
    @Override
    @Transactional
    public void resetPassword(Long userId, String passwordHash) {
        requireCurrentTenant(userId);
        if (passwordHash == null || passwordHash.isBlank()) throw new BusinessException("新密码不能为空");
        userMapper.updatePassword(userId, passwordHash);
        userMapper.updateForceChangePassword(userId, 1);
        userMapper.incrementAuthVersion(userId);
    }

    // 确保引导管理员存在。
    @Override
    @Transactional
    public SysUser ensureBootstrapAdmin(String username, String passwordHash) {
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (existing != null) {
            return existing;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setTenantId(1L);
        user.setPasswordHash(passwordHash);
        user.setNickname("系统管理员");
        user.setStatus(UserLifecycle.ACTIVE);
        user.setUserType("INTERNAL");
        userMapper.insert(user);
        Long adminRoleId = roleMapper.selectIdByRoleCode(1L, "admin");
        if (adminRoleId != null) {
            userRoleMapper.insertRole(user.getId(), adminRoleId);
        }
        return user;
    }

    // 升级用户密码散列。
    @Override
    public void upgradePassword(Long userId, String passwordHash) {
        userMapper.updatePassword(userId, passwordHash);
        userMapper.updateForceChangePassword(userId, 0);
        userMapper.incrementAuthVersion(userId);
    }

    // 确保演示用户存在。
    @Override
    @Transactional
    public SysUser ensureDemoUser(String username, String passwordHash, String nickname, String roleCode, String userType) {
        String normalizedUserType = "EXTERNAL".equals(userType) ? "EXTERNAL" : "INTERNAL";
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (existing != null) {
            if (!normalizedUserType.equals(existing.getUserType())) {
                existing.setUserType(normalizedUserType);
                userMapper.updateById(existing);
            }
            upgradePassword(existing.getId(), passwordHash);
            return existing;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setTenantId(1L);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setStatus(UserLifecycle.ACTIVE);
        user.setUserType(normalizedUserType);
        userMapper.insert(user);
        Long roleId = roleMapper.selectIdByRoleCode(1L, roleCode);
        if (roleId == null) {
            throw new BusinessException("演示角色不存在：" + roleCode);
        }
        userRoleMapper.insertRole(user.getId(), roleId);
        return user;
    }

    // 替换用户角色并刷新授权版本。
    @Override
    @Transactional
    public void replaceUserRoles(Long userId, List<String> roleCodes) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (TenantContext.tenantId() != user.getTenantId()) {
            throw new BusinessException("不能修改其他企业的用户角色");
        }
        if (TenantContext.userId() != null && TenantContext.userId().equals(userId)) throw new BusinessException("禁止修改自己的系统角色");
        List<String> normalizedRoles = roleCodes == null ? List.of() : roleCodes.stream()
                .filter(code -> code != null && !code.isBlank()).map(code -> code.trim().toLowerCase()).distinct().toList();
        if (normalizedRoles.stream().anyMatch(this::protectedRole)) {
            throw new BusinessException("受保护系统角色必须通过双人授权申请授予");
        }
        ensureLastSuperadminIsNotRemoved(userId, normalizedRoles);
        userRoleMapper.deleteByUserId(userId);
        for (String roleCode : normalizedRoles) {
            Long roleId = roleMapper.selectIdByRoleCode(user.getTenantId(), roleCode);
            if (roleId == null) {
                throw new BusinessException("角色不存在：" + roleCode);
            }
            userRoleMapper.insertRole(userId, roleId);
        }
        userMapper.incrementAuthVersion(userId);
        audit(userId, "SYSTEM_ROLE_REPLACED", null, String.join(",", normalizedRoles), "系统角色授权变更");
    }

    // 使指定角色关联用户的旧会话失效。
    @Override
    @Transactional
    public void invalidateUsersByRole(Long roleId) {
        userMapper.incrementAuthVersionByRoleId(TenantContext.tenantId(), roleId);
    }

    private SysUser requireCurrentTenant(Long userId) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!Long.valueOf(TenantContext.tenantId()).equals(user.getTenantId())) throw new BusinessException("不能维护其他企业的用户");
        return user;
    }

    private void verifyDepartment(Long deptId) {
        if (deptId == null) return;
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null || !Long.valueOf(TenantContext.tenantId()).equals(dept.getTenantId()) || !"ENABLED".equals(dept.getStatus())) {
            throw new BusinessException("部门不存在、已停用或不属于当前企业");
        }
    }

    private int count(String sql, Long userId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, TenantContext.tenantId(), userId);
        return value == null ? 0 : value;
    }

    private boolean protectedRole(String roleCode) {
        return Set.of("superadmin", "system_admin", "hr_admin").contains(roleCode);
    }

    private void ensureLastSuperadminIsNotRemoved(Long userId, List<String> roles) {
        Long superadminId = roleMapper.selectIdByRoleCode(TenantContext.tenantId(), "superadmin");
        if (superadminId == null || roles.contains("superadmin") || !userRoleMapper.selectUserIdsByRoleId(superadminId).contains(userId)) return;
        if (userRoleMapper.selectUserIdsByRoleId(superadminId).size() <= 1) {
            throw new BusinessException("不能移除最后一个超级管理员");
        }
    }

    private void audit(Long subjectId, String changeType, String before, String after, String reason) {
        jdbc.update("INSERT INTO nso_authorization_audit (tenant_id,subject_type,subject_id,change_type,before_json,after_json,reason,operator_id) " +
                        "VALUES (?,?,?, ?, JSON_OBJECT('value', ?), JSON_OBJECT('value', ?), ?, ?)",
                TenantContext.tenantId(), "USER", subjectId, changeType, before, after, reason,
                TenantContext.userId());
    }
}
