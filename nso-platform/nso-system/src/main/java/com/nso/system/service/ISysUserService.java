package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.system.domain.SysUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统用户与授权信息服务接口。
 */
public interface ISysUserService {

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 按编号查询用户。
     *
     * @param userId 用户编号
     * @return 用户信息
     */
    Optional<SysUser> findById(Long userId);

    /**
     * 查询用户角色标识。
     *
     * @param userId 用户编号
     * @return 角色标识列表
     */
    List<String> roleCodes(Long userId);

    /**
     * 查询用户权限标识。
     *
     * @param userId 用户编号
     * @return 权限标识列表
     */
    List<String> permissionCodes(Long userId);

    /**
     * 查询用户菜单路由。
     *
     * @param userId 用户编号
     * @return 菜单路由列表
     */
    List<String> menuRoutes(Long userId);

    /**
     * 查询用户授权版本。
     *
     * @param userId 用户编号
     * @return 授权版本号
     */
    int authVersion(Long userId);

    /**
     * 查询用户列表。
     *
     * @return 用户列表
     */
    List<SysUser> listUsers();

    /**
     * 分页查询用户列表。
     *
     * @param pageQuery 分页参数
     * @return 用户分页结果
     */
    PageResult<SysUser> listUsers(PageQuery pageQuery);

    /**
     * 创建系统用户。
     *
     * @param username 用户名
     * @param passwordHash 密码散列
     * @param nickname 用户昵称
     * @param deptId 部门编号
     * @param roleCodes 角色标识列表
     * @return 新建的用户
     */
    SysUser createUser(String username, String passwordHash, String nickname, Long deptId, List<String> roleCodes);

    /**
     * 更新系统用户。
     *
     * @param userId 用户编号
     * @param nickname 用户昵称
     * @param deptId 部门编号
     * @param roleCodes 角色标识列表
     * @param status 用户状态
     * @return 更新后的用户
     */
    SysUser updateUser(Long userId, String nickname, Long deptId, List<String> roleCodes, String status);

    /**
     * 激活用户并设置初始权限。
     *
     * @param userId 用户编号
     * @param passwordHash 密码散列
     * @param roleCodes 角色标识列表
     * @param reason 激活原因
     * @return 激活后的用户
     */
    SysUser activateUser(Long userId, String passwordHash, List<String> roleCodes, String reason);

    /**
     * 检查用户离岗前的业务交接项。
     *
     * @param userId 用户编号
     * @return 待交接业务数量
     */
    Map<String, Integer> handoverCheck(Long userId);

    /**
     * 完成用户生命周期状态变更。
     *
     * @param userId 用户编号
     * @param status 目标状态
     * @param reason 变更原因
     * @return 更新后的用户
     */
    SysUser completeLifecycle(Long userId, String status, String reason);

    /**
     * 分配员工编号。
     *
     * @param userId 用户编号
     * @param employeeNo 员工编号
     */
    void assignEmployeeNo(Long userId, String employeeNo);

    /**
     * 重置用户密码。
     *
     * @param userId 用户编号
     * @param passwordHash 密码散列
     */
    void resetPassword(Long userId, String passwordHash);

    /**
     * 确保引导管理员存在。
     *
     * @param username 用户名
     * @param passwordHash 密码散列
     * @return 管理员用户
     */
    SysUser ensureBootstrapAdmin(String username, String passwordHash);

    /**
     * 确保演示用户存在。
     *
     * @param username 用户名
     * @param passwordHash 密码散列
     * @param nickname 用户昵称
     * @param roleCode 角色标识
     * @param userType 身份类型
     * @return 演示用户
     */
    SysUser ensureDemoUser(String username, String passwordHash, String nickname, String roleCode, String userType);

    /**
     * 替换用户角色并刷新授权版本。
     *
     * @param userId 用户编号
     * @param roleCodes 角色标识列表
     */
    void replaceUserRoles(Long userId, List<String> roleCodes);

    /**
     * 使指定角色关联用户的旧会话失效。
     *
     * @param roleId 角色编号
     */
    void invalidateUsersByRole(Long roleId);

    /**
     * 升级用户密码散列。
     *
     * @param userId 用户编号
     * @param passwordHash 新密码散列
     */
    void upgradePassword(Long userId, String passwordHash);
}
