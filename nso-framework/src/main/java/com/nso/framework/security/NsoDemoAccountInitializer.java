package com.nso.framework.security;

import com.nso.framework.config.NsoDemoProperties;
import com.nso.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class NsoDemoAccountInitializer implements ApplicationRunner {
    private final NsoDemoProperties properties;
    private final ISysUserService users;
    private final PasswordEncoder passwordEncoder;

    public NsoDemoAccountInitializer(NsoDemoProperties properties, ISysUserService users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        String password = properties.getDefaultPassword();
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("NSO_DEMO_ENABLED requires an NSO_DEMO_DEFAULT_PASSWORD of at least 8 characters");
        }
        String hash = passwordEncoder.encode(password);
        ensure("demo-admin", "演示系统管理员", "admin", hash);
        ensure("demo-pm", "演示销售项目经理", "project_manager", hash);
        ensure("demo-tech", "演示技术设计", "technical", hash);
        ensure("demo-process", "演示工艺人员", "process", hash);
        ensure("demo-purchase", "演示采购人员", "purchaser", hash);
        ensure("demo-production", "演示计划生产", "production", hash);
        ensure("demo-quality", "演示质量人员", "quality", hash);
        ensure("demo-field", "演示现场人员", "field_user", hash);
        ensure("demo-customer", "演示客户确认人", "customer_confirm", hash);
        ensure("demo-executive", "演示管理层", "executive", hash);
    }

    private void ensure(String username, String nickname, String roleCode, String passwordHash) {
        users.ensureDemoUser(username, passwordHash, nickname, roleCode);
    }
}
