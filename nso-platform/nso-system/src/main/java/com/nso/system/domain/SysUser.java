package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

// 系统用户实体。
@TableName("sys_user")
public class SysUser {

    // 用户编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 部门编号
    private Long deptId;

    // 登录用户名
    private String username;

    // 员工编号
    private String employeeNo;

    // 密码散列
    private String passwordHash;

    // 是否强制修改密码
    private Integer forceChangePassword;

    // 用户昵称
    private String nickname;

    // 手机号码
    private String phone;

    // 邮箱地址
    private String email;

    // 性别
    private String gender;

    // 头像文件编号
    private Long avatarFileId;

    // 账号状态
    private String status;

    // 状态变更原因
    private String statusReason;

    // 临时状态截止时间
    private LocalDateTime statusEffectiveUntil;

    // 用户类型
    private String userType;

    // 数据版本
    @Version
    private Integer version;

    // 授权版本
    private Integer authVersion;

    // 创建时间
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getForceChangePassword() {
        return forceChangePassword;
    }

    public void setForceChangePassword(Integer forceChangePassword) {
        this.forceChangePassword = forceChangePassword;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getAvatarFileId() {
        return avatarFileId;
    }

    public void setAvatarFileId(Long avatarFileId) {
        this.avatarFileId = avatarFileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LocalDateTime getStatusEffectiveUntil() {
        return statusEffectiveUntil;
    }

    public void setStatusEffectiveUntil(LocalDateTime statusEffectiveUntil) {
        this.statusEffectiveUntil = statusEffectiveUntil;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Integer authVersion) {
        this.authVersion = authVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
