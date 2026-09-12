package com.nso.framework.security;

import com.nso.framework.config.NsoDemoProperties;
import com.nso.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 演示账号初始化器。
@Component
@Order(20)
public class NsoDemoAccountInitializer implements ApplicationRunner {

    // NSO演示配置
    private final NsoDemoProperties properties;
    // 系统用户服务
    private final ISysUserService users;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;

    public NsoDemoAccountInitializer(
            NsoDemoProperties properties,
            ISysUserService users,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    // 按稳定用户名初始化演示账号。
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        String password = properties.getDefaultPassword();
        if (password == null || password.length() < 8) {
            throw new IllegalStateException(
                    "NSO_DEMO_ENABLED requires an NSO_DEMO_DEFAULT_PASSWORD of at least 8 characters");
        }
        String hash = passwordEncoder.encode(password);
        ensure("admin", "系统管理员", "admin", "INTERNAL", hash);
        ensure("demo-admin", "系统管理员", "admin", "INTERNAL", hash);
        ensure("demo-pm", "陈晓明", "project_manager", "INTERNAL", hash);
        ensure("demo-tech", "技术设计工程师", "technical", "INTERNAL", hash);
        ensure("demo-process", "工艺工程师", "process", "INTERNAL", hash);
        ensure("demo-purchase", "采购专员", "purchaser", "INTERNAL", hash);
        ensure("demo-production", "生产计划员", "production", "INTERNAL", hash);
        ensure("demo-quality", "质量工程师", "quality", "INTERNAL", hash);
        ensure("demo-field", "现场执行员", "field_user", "INTERNAL", hash);
        ensure("demo-executive", "经营负责人", "executive", "INTERNAL", hash);
        ensure("demo-customer", "演示客户确认人", "customer_confirm", "EXTERNAL", hash);
    }

    private void ensure(String username, String nickname, String roleCode, String userType, String passwordHash) {
        users.ensureDemoUser(username, passwordHash, nickname, roleCode, userType);
    }
}
