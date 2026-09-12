package com.nso;

import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.framework.security.RequestRateLimitService;
import com.nso.framework.security.LoginProtectionService;
import com.nso.framework.security.NsoDemoScenarioInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "nso.bootstrap.enabled=true",
        "nso.bootstrap.admin-username=api-admin",
        "nso.bootstrap.admin-password=api-admin-password",
        "nso.demo.enabled=true",
        "nso.demo.default-password=admin123"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
// Shared Testcontainers-backed fixture. Category-specific test classes invoke the operations below so CI reports regressions by pilot release gate.
abstract class NsoApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CustomerMapper customers;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestRateLimitService requestRateLimitService;

    @Autowired
    private LoginProtectionService loginProtectionService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NsoDemoScenarioInitializer demoScenarios;

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("nso_platform")
            .withUsername("nso_app")
            .withPassword("nso_app_change_me");

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        startSharedContainers();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("nso.minio.endpoint", () -> "http://127.0.0.1:9000");
        registry.add("nso.minio.access-key", () -> "test-access-key");
        registry.add("nso.minio.secret-key", () -> "test-secret-key");
    }

    // The category test classes share Spring's cached context. Testcontainers' {@code @Container} lifecycle would restart inherited containers between classes and invalidate the cached Redis/MySQL ports, so keep this pair alive for the whole Surefire JVM instead.
    private static synchronized void startSharedContainers() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    void contextLoads() {
    }

    // V1 数据必须能够通过不可变的 V1-V6 迁移链升级， 并接收 V7 治理数据回填。该测试使用全新的 MySQL 容器， 与现有 V1 数据库的升级路径一致。
    void migratesV1DataToTenantGovernanceAndKeepsRequestIdempotency() {
        Integer latestVersion = jdbc.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1", Integer.class);
        Integer defaultTenants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM nso_tenant WHERE id = 1 AND tenant_code = 'DEFAULT' AND status = 'ENABLED'", Integer.class);
        Integer governedUsers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'tenant_id'", Integer.class);
        Integer governedFiles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'nso_file_object' AND column_name = 'tenant_id'", Integer.class);
        Integer governedProjectFiles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'nso_file_object' AND column_name = 'project_id'", Integer.class);

        Integer departments = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dept WHERE tenant_id = 1 AND deleted = 0", Integer.class);
        Integer menus = jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE deleted = 0", Integer.class);

        Integer roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE tenant_id = 1 AND status = 'ENABLED' AND deleted = 0", Integer.class);
        Integer roleMenuCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu", Integer.class);
        Integer demoUserCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username LIKE 'demo-%' AND user_type = 'INTERNAL'", Integer.class);
        String demoCustomerType = jdbc.queryForObject("SELECT user_type FROM sys_user WHERE username = 'demo-customer'", String.class);

        Integer userTypeColumn = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'user_type'", Integer.class);
        Integer responsibilityTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_project_member_responsibility'", Integer.class);
        Integer managerHistoryTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_project_manager_history'", Integer.class);
        Integer salesRole = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE tenant_id = 1 AND role_code = 'sales' AND status = 'ENABLED' AND deleted = 0", Integer.class);
        Integer externalProjectAccessTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_external_project_access'", Integer.class);
        Integer identityContactColumn = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'nso_external_identity' AND column_name = 'contact_id'", Integer.class);

        Integer recoveryTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_password_recovery_request'", Integer.class);
        Integer supportTicketTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_support_ticket'", Integer.class);
        Integer supportAttachmentTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_support_ticket_attachment'", Integer.class);
        Integer actionProjectionTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_action_item'", Integer.class);
        Integer exceptionCaseTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_exception_case'", Integer.class);
        Integer approvalTemplateTable = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'nso_approval_template'", Integer.class);
        Integer capaTaskColumn = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'nso_task' AND column_name = 'capa_case_id'", Integer.class);
        Integer dashboardMessageIndexColumns = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'nso_message' "
                + "AND index_name = 'idx_message_dashboard_receiver_status_created'", Integer.class);
        Integer projectManagerExecutePermission = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role r "
                + "JOIN sys_role_menu rm ON rm.role_id = r.id "
                + "JOIN sys_menu m ON m.id = rm.menu_id "
                + "WHERE r.tenant_id = 1 AND r.role_code = 'project_manager' "
                + "AND m.permission_code = 'task:execute' AND m.deleted = 0", Integer.class);
        Integer projectManagerFeedbackPermission = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role r "
                + "JOIN sys_role_menu rm ON rm.role_id = r.id "
                + "JOIN sys_menu m ON m.id = rm.menu_id "
                + "WHERE r.tenant_id = 1 AND r.role_code = 'project_manager' "
                + "AND m.permission_code = 'task:feedback' AND m.deleted = 0", Integer.class);
        Integer staleProjectMemberNames = jdbc.queryForObject("SELECT COUNT(*) FROM nso_project_member pm "
                + "JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id "
                + "WHERE pm.tenant_id = 1 AND pm.deleted = 0 AND u.username LIKE 'demo-%' "
                + "AND NOT (pm.member_name <=> u.nickname)", Integer.class);
        List<Map<String, Object>> staleTaskNames = jdbc.queryForList("SELECT t.task_no, t.responsible_name, "
                + "u.username, u.nickname FROM nso_task t "
                + "JOIN sys_user u ON u.id = t.assignee_id AND u.tenant_id = t.tenant_id "
                + "WHERE t.tenant_id = 1 AND t.deleted = 0 AND t.task_no LIKE 'TSK-DEMO-%' "
                + "AND NOT (t.responsible_name <=> u.nickname)");

        assertThat(latestVersion).isEqualTo(40);
        assertThat(defaultTenants).isEqualTo(1);
        assertThat(governedUsers).isEqualTo(1);
        assertThat(governedFiles).isEqualTo(1);
        assertThat(governedProjectFiles).isEqualTo(1);
        assertThat(departments).isGreaterThan(0);
        assertThat(menus).isGreaterThan(0);
        assertThat(roleCount).isGreaterThanOrEqualTo(9);
        assertThat(roleMenuCount).isGreaterThan(0);
        assertThat(demoUserCount).isEqualTo(9);
        assertThat(demoCustomerType).isEqualTo("EXTERNAL");
        assertThat(userTypeColumn).isEqualTo(1);
        assertThat(responsibilityTable).isEqualTo(1);
        assertThat(managerHistoryTable).isEqualTo(1);
        assertThat(salesRole).isEqualTo(1);
        assertThat(externalProjectAccessTable).isEqualTo(1);
        assertThat(identityContactColumn).isEqualTo(1);
        assertThat(recoveryTable).isEqualTo(1);
        assertThat(supportTicketTable).isEqualTo(1);
        assertThat(supportAttachmentTable).isEqualTo(1);
        assertThat(actionProjectionTable).isEqualTo(1);
        assertThat(exceptionCaseTable).isEqualTo(1);
        assertThat(approvalTemplateTable).isEqualTo(1);
        assertThat(capaTaskColumn).isEqualTo(1);
        assertThat(dashboardMessageIndexColumns).isEqualTo(5);
        assertThat(projectManagerExecutePermission).isEqualTo(1);
        assertThat(projectManagerFeedbackPermission).isEqualTo(1);
        assertThat(staleProjectMemberNames).isZero();
        assertThat(staleTaskNames).isEmpty();

        jdbc.update("INSERT INTO nso_idempotency_key (tenant_id, user_id, request_id, request_method, request_path, status) VALUES (1, 9, 'upgrade-test-request', 'POST', '/api/v1/admin/projects', 'COMPLETE')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO nso_idempotency_key (tenant_id, user_id, request_id, request_method, request_path, status) VALUES (1, 9, 'upgrade-test-request', 'POST', '/api/v1/admin/projects', 'COMPLETE')"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    void tenantInterceptorPreventsCrossTenantCustomerRead() {
        TenantContext.set(new TenantContext.Actor(11L, 1101L, "tenant-a", List.of("admin")));
        try {
            Customer customer = new Customer();
            customer.setTenantId(11L);
            customer.setCustomerCode("TEST-TENANT-" + System.nanoTime());
            customer.setName("tenant A customer");
            customer.setStatus("ACTIVE");
            customers.insert(customer);

            TenantContext.set(new TenantContext.Actor(12L, 1201L, "tenant-b", List.of("project_manager")));
            assertThat(customers.selectById(customer.getId())).isNull();

            TenantContext.set(new TenantContext.Actor(11L, 1101L, "tenant-a", List.of("admin")));
            assertThat(customers.selectById(customer.getId())).isNotNull();
        } finally {
            TenantContext.clear();
        }
    }

    void protectedApiReturnsStructuredUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期或尚未登录"));
    }

    void publicAccountSupportEndpointsHideAccountExistenceAndAdminCanReadTicketQueue() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/password-recovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing-" + System.nanoTime() + "\",\"contactName\":\"匿名申请人\",\"contactValue\":\"13800000000\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/admin/auth/support-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactName\":\"匿名申请人\",\"contactValue\":\"13800000000\",\"category\":\"FUNCTION\",\"priority\":\"NORMAL\",\"description\":\"登录页功能联调工单\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ticketNo").value(org.hamcrest.Matchers.startsWith("SUP-")));

        mockMvc.perform(get("/api/v1/admin/system/account-support/support-tickets")
                        .header("Authorization", bearer(loginToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].ticketNo").isString());
    }

    void securityControlsRejectInvalidAndRevokedTokensRefreshReplayAndFailedLoginBursts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects").header("Authorization", bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        LoginTokens revokedSession = loginTokens("api-admin", "api-admin-password");
        mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .header("Authorization", bearer(revokedSession.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + revokedSession.refreshToken() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/projects").header("Authorization", bearer(revokedSession.accessToken())))
                .andExpect(status().isUnauthorized());

        LoginTokens refreshSession = loginTokens("api-admin", "api-admin-password");
        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshSession.refreshToken() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshSession.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());

        String blockedUsername = "blocked-" + System.nanoTime();
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + blockedUsername + "\",\"password\":\"bad-password\"}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + blockedUsername + "\",\"password\":\"bad-password\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value(423));
    }

    void rateLimiterRejectsRequestsAfterTheConfiguredWindowQuota() {
        String scope = "test-" + System.nanoTime();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isFalse();
    }

    void rateLimiterRepairsLegacyKeyWithoutExpiration() throws Exception {
        String scope = "legacy-ttl-" + System.nanoTime();
        String identity = "127.0.0.1";
        String key = "nso:security:rate:" + scope + ":" + java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
        redisTemplate.opsForValue().set(key, "99");

        assertThat(redisTemplate.getExpire(key)).isEqualTo(-1);
        assertThat(requestRateLimitService.tryAcquire(scope, identity, 100, Duration.ofMinutes(1))).isTrue();
        assertThat(redisTemplate.getExpire(key)).isPositive();
    }

    void loginFailureCounterRepairsLegacyKeyWithoutExpiration() throws Exception {
        String username = "legacy-login-" + System.nanoTime();
        String subject = username + ":admin";
        String key = "nso:security:login:failed:" + java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8)));
        redisTemplate.opsForValue().set(key, "1");

        assertThat(redisTemplate.getExpire(key)).isEqualTo(-1);
        loginProtectionService.recordFailure(username, "admin");
        assertThat(redisTemplate.getExpire(key)).isPositive();
    }

    void authenticatedUserCanCreateCustomerProjectRequirementAndReview() throws Exception {
        String token = loginToken();
        String customerResponse = mockMvc.perform(post("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "customer-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"联调客户\",\"industry\":\"装备制造\",\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        Number customerIdValue = com.jayway.jsonpath.JsonPath.read(customerResponse, "$.data.id");
        long customerId = customerIdValue.longValue();

        String projectResponse = mockMvc.perform(post("/api/v1/admin/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "project-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"productName\":\"非标装配工装\",\"quantity\":2,\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        Number projectIdValue = com.jayway.jsonpath.JsonPath.read(projectResponse, "$.data.id");
        long projectId = projectIdValue.longValue();

        mockMvc.perform(post("/api/v1/admin/projects/{id}/requirements", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "requirement-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"SIZE\",\"content\":\"工装定位尺寸公差±0.1mm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/admin/projects/{id}/submit-review", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "review-" + System.nanoTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWING"))
                .andExpect(jsonPath("$.data.stage").value("TECHNICAL"));

        mockMvc.perform(get("/api/v1/admin/reports/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.projects").isNumber())
                .andExpect(jsonPath("$.data.metrics.highRisks").isNumber())
                .andExpect(jsonPath("$.data.metrics.waitingSamples").isNumber())
                .andExpect(jsonPath("$.data.metrics.unreadMessages").isNumber())
                .andExpect(jsonPath("$.data.metrics.overdueTasks").isNumber());

        mockMvc.perform(get("/api/v1/admin/workbench").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortcuts[0].route").value("/projects"))
                .andExpect(jsonPath("$.data.shortcuts[1].route").value("/samples"))
                .andExpect(jsonPath("$.data.shortcuts[2].route").value("/changes"));
    }

    void projectListReturnsRealChangeCountsPerProject() throws Exception {
        BasicProject changedProject = createBasicProject();
        BasicProject untouchedProject = createBasicProject();

        for (int index = 1; index <= 2; index++) {
            mockMvc.perform(post("/api/v1/admin/changes")
                            .header("Authorization", bearer(changedProject.adminToken()))
                            .header("X-Request-Id", "project-change-count-" + index + "-" + System.nanoTime())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"projectId\":" + changedProject.projectId()
                                    + ",\"changeType\":\"DESIGN\",\"urgency\":\"NORMAL\",\"afterContent\":\"变更内容 "
                                    + index + "\",\"reason\":\"验证项目变更聚合\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(changedProject.projectId()));
        }

        String changedProjectNo = jdbc.queryForObject(
                "SELECT project_no FROM nso_project WHERE tenant_id=1 AND id=?", String.class, changedProject.projectId());
        String untouchedProjectNo = jdbc.queryForObject(
                "SELECT project_no FROM nso_project WHERE tenant_id=1 AND id=?", String.class, untouchedProject.projectId());

        mockMvc.perform(get("/api/v1/admin/projects")
                        .header("Authorization", bearer(changedProject.adminToken()))
                        .param("keyword", changedProjectNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(changedProject.projectId()))
                .andExpect(jsonPath("$.data.list[0].changeCount").value(2));

        mockMvc.perform(get("/api/v1/admin/projects")
                        .header("Authorization", bearer(untouchedProject.adminToken()))
                        .param("keyword", untouchedProjectNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(untouchedProject.projectId()))
                .andExpect(jsonPath("$.data.list[0].changeCount").value(0));
    }

    void administratorCanMaintainOrganizationRolesMenusAndUsers() throws Exception {
        String token = loginToken();
        String suffix = String.valueOf(System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/system/departments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].deptName").value("默认部门"));

        mockMvc.perform(post("/api/v1/admin/system/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptName\":\"研发部" + suffix + "\",\"sortNo\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(1));

        String roleCode = "designer_" + suffix;
        mockMvc.perform(post("/api/v1/admin/system/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"" + roleCode + "\",\"roleName\":\"测试设计师\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value(roleCode));

        mockMvc.perform(post("/api/v1/admin/system/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"测试菜单\",\"permissionCode\":\"test:" + suffix + "\",\"routePath\":\"/test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value("test:" + suffix));

        mockMvc.perform(post("/api/v1/admin/system/dicts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test_type\",\"code\":\"code_" + suffix + "\",\"label\":\"测试字典\",\"sortNo\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("code_" + suffix));

        mockMvc.perform(get("/api/v1/admin/system/dicts").param("type", "test_type").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].dict_code").value("code_" + suffix));

        mockMvc.perform(post("/api/v1/admin/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"designer" + suffix + "\",\"password\":\"initial-password\",\"nickname\":\"测试用户\",\"roleCodes\":[\"" + roleCode + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("designer" + suffix));

        mockMvc.perform(get("/api/v1/admin/system/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].passwordHash").doesNotExist());
    }

    void listEndpointsUseNormalizedPageResultContract() throws Exception {
        String token = loginToken();

        for (String path : List.of(
                "/api/v1/admin/projects",
                "/api/v1/admin/samples",
                "/api/v1/admin/changes",
                "/api/v1/admin/tasks",
                "/api/v1/admin/risks",
                "/api/v1/admin/system/users",
                "/api/v1/admin/audit-logs")) {
            mockMvc.perform(get(path)
                            .header("Authorization", "Bearer " + token)
                            .param("pageNo", "999")
                            .param("pageSize", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.total").isNumber())
                    .andExpect(jsonPath("$.data.pageNo").value(999))
                    .andExpect(jsonPath("$.data.pageSize").value(100))
                    .andExpect(jsonPath("$.data.list.length()").value(0));
        }
    }

    void projectMembersUseDirectoryIdentityAndSupportResponsibilityLifecycle() throws Exception {
        BasicProject project = createBasicProject();
        long technicalUserId = userId("demo-tech");

        mockMvc.perform(get("/api/v1/admin/projects/manager-candidates")
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());

        String memberResponse = mockMvc.perform(post("/api/v1/admin/projects/{id}/members", project.projectId())
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + technicalUserId + ",\"responsibilityCodes\":[\"TECHNICAL\"],\"primaryResponsibilityCode\":\"TECHNICAL\",\"memberName\":\"手填姓名应被忽略\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(technicalUserId))
                .andExpect(jsonPath("$.data.memberName").value("技术设计工程师"))
                .andExpect(jsonPath("$.data.responsibilityCodes[0]").value("TECHNICAL"))
                .andReturn().getResponse().getContentAsString();
        long memberId = ((Number) com.jayway.jsonpath.JsonPath.read(memberResponse, "$.data.id")).longValue();

        mockMvc.perform(put("/api/v1/admin/projects/{projectId}/members/{memberId}", project.projectId(), memberId)
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsibilityCodes\":[\"TECHNICAL\"],\"primaryResponsibilityCode\":\"TECHNICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryResponsibilityCode").value("TECHNICAL"));

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/members/{memberId}/remove", project.projectId(), memberId)
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REMOVED"));
        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/members/{memberId}/restore", project.projectId(), memberId)
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    void replacingProjectTechnicalOwnerReleasesTheExistingUniqueOwnerSlot() throws Exception {
        BasicProject project = createBasicProject();
        addProjectMember(project, "demo-tech", "TECH_OWNER");
        addProjectMember(project, "demo-admin", "TECH_OWNER");

        Integer ownerCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_project_member_responsibility "
                        + "WHERE tenant_id=1 AND project_id=? AND owner_slot=? AND status='ACTIVE' AND deleted=0",
                Integer.class, project.projectId(), project.projectId() + ":TECH_OWNER");
        String formerOwnerCode = jdbc.queryForObject("SELECT responsibility_code FROM nso_project_member_responsibility "
                        + "WHERE tenant_id=1 AND project_id=? AND user_id=? AND status='ACTIVE' AND deleted=0",
                String.class, project.projectId(), userId("demo-tech"));
        String formerOwnerSlot = jdbc.queryForObject("SELECT owner_slot FROM nso_project_member_responsibility "
                        + "WHERE tenant_id=1 AND project_id=? AND user_id=? AND status='ACTIVE' AND deleted=0",
                String.class, project.projectId(), userId("demo-tech"));

        assertThat(ownerCount).isEqualTo(1);
        assertThat(formerOwnerCode).isEqualTo("TECH_MEMBER");
        assertThat(formerOwnerSlot).isNull();
    }

    void demoRolesExposeOnlyTheirGrantedFunctionsAndPermissionChangesRevokeOldTokens() throws Exception {
        LoginTokens executiveSession = loginTokens("demo-executive", "admin123");
        String executiveToken = executiveSession.accessToken();
        mockMvc.perform(get("/api/v1/admin/reports/overview").header("Authorization", "Bearer " + executiveToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/admin/system/users").header("Authorization", "Bearer " + executiveToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-customer\",\"password\":\"admin123\"}"))
                .andExpect(status().is4xxClientError());

        Integer deniedAudits = jdbc.queryForObject("SELECT COUNT(*) FROM nso_audit_log WHERE user_name='demo-executive' AND operation_type='ACCESS_DENIED' AND result='DENIED'", Integer.class);
        Integer sensitiveAuditValues = jdbc.queryForObject("SELECT COUNT(*) FROM nso_audit_log WHERE after_summary LIKE '%admin123%' OR after_summary LIKE '%eyJ%'", Integer.class);
        assertThat(deniedAudits).isGreaterThan(0);
        assertThat(sensitiveAuditValues).isZero();

        Long executiveRoleId = jdbc.queryForObject("SELECT id FROM sys_role WHERE tenant_id=1 AND role_code='executive'", Long.class);
        mockMvc.perform(put("/api/v1/admin/system/roles/{roleId}/permissions", executiveRoleId)
                        .header("Authorization", "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionCodes\":[\"dashboard:view\",\"project:view\",\"risk:view\",\"report:view\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/admin/reports/overview").header("Authorization", "Bearer " + executiveToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + executiveSession.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    void fieldRoleAndAdministrativeToolsHonorTheV12PermissionBoundary() throws Exception {
        for (String username : List.of("demo-admin", "demo-pm", "demo-tech", "demo-process", "demo-purchase", "demo-production", "demo-quality", "demo-field", "demo-executive")) {
            mockMvc.perform(get("/api/v1/admin/auth/me").header("Authorization", bearer(loginToken(username, "admin123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value(username));
        }

        String fieldToken = loginToken("demo-field", "admin123");
        mockMvc.perform(get("/api/v1/admin/projects").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/monitor/jobs").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/tool/gen/tables").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isForbidden());
    }

    void profileUpdatesAreScopedOptimisticAndPasswordChangeRevokesTheCurrentSession() throws Exception {
        String adminToken = loginToken();
        String suffix = Long.toUnsignedString(System.nanoTime());
        String username = "profile-user-" + suffix;
        String oldPassword = "profile-old-password";
        String newPassword = "profile-new-password";
        mockMvc.perform(post("/api/v1/admin/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + oldPassword
                                + "\",\"nickname\":\"Profile User\",\"roleCodes\":[\"field_user\"]}"))
                .andExpect(status().isOk());
        activateInternalUser(username, adminToken, "activation-profile-password", List.of("field_user"));

        String token = completeForcedPasswordChange(username, "activation-profile-password", oldPassword);
        String profileResponse = mockMvc.perform(get("/api/v1/admin/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.roles[0]").value("field_user"))
                .andReturn().getResponse().getContentAsString();
        int initialVersion = ((Number) com.jayway.jsonpath.JsonPath.read(profileResponse, "$.data.version")).intValue();

        mockMvc.perform(put("/api/v1/admin/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Updated Profile\",\"phone\":\"13900009999\",\"email\":\"profile@nso.local\",\"gender\":\"FEMALE\",\"version\":" + initialVersion + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Updated Profile"))
                .andExpect(jsonPath("$.data.version").value(initialVersion + 1));
        mockMvc.perform(put("/api/v1/admin/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Stale Profile\",\"gender\":\"UNSPECIFIED\",\"version\":" + initialVersion + "}"))
                .andExpect(status().isBadRequest());

        MockMultipartFile invalidType = new MockMultipartFile("file", "avatar.txt", "text/plain", "not-an-image".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/profile/avatar").file(invalidType).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        MockMultipartFile oversized = new MockMultipartFile("file", "avatar.png", "image/png", new byte[2 * 1024 * 1024 + 1]);
        mockMvc.perform(multipart("/api/v1/admin/profile/avatar").file(oversized).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        long profileUserId = userId(username);
        String foreignAvatarPath = "tests/other-user-avatar-" + suffix + ".png";
        jdbc.update("INSERT INTO nso_file_object (file_name, content_type, file_size, sha256, storage_path, tenant_id, project_id, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "other-user-avatar.png", "image/png", 1L, "test-avatar", foreignAvatarPath, 1L, null, userId("api-admin"));
        Long foreignFileId = jdbc.queryForObject("SELECT id FROM nso_file_object WHERE tenant_id=1 AND storage_path=?", Long.class, foreignAvatarPath);
        jdbc.update("UPDATE sys_user SET avatar_file_id=? WHERE id=?", foreignFileId, profileUserId);
        mockMvc.perform(get("/api/v1/admin/profile/avatar").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/profile").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/profile").header("Authorization", bearer(loginToken(username, newPassword))))
                .andExpect(status().isOk());
    }

    void demoScenarioInitializerCanRunAgainWithoutDuplicateRoleFixtures() throws Exception {
        int beforeMembers = count("SELECT COUNT(*) FROM nso_project_member WHERE tenant_id=1 AND project_id IN (SELECT id FROM nso_project WHERE tenant_id=1 AND project_no LIKE 'NSO-DEMO-%')");
        int beforeTasks = count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND task_no LIKE 'TSK-DEMO-%'");
        int beforeMessages = count("SELECT COUNT(*) FROM nso_message WHERE tenant_id=1 AND title LIKE '%待%'");
        demoScenarios.run(new DefaultApplicationArguments(new String[0]));
        assertThat(count("SELECT COUNT(*) FROM nso_project_member WHERE tenant_id=1 AND project_id IN (SELECT id FROM nso_project WHERE tenant_id=1 AND project_no LIKE 'NSO-DEMO-%')")).isEqualTo(beforeMembers);
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND task_no LIKE 'TSK-DEMO-%'")).isEqualTo(beforeTasks);
        assertThat(count("SELECT COUNT(*) FROM nso_message WHERE tenant_id=1 AND title LIKE '%待%'")).isEqualTo(beforeMessages);
    }

    void customerAndPublicConfirmationEndpointsRedactInternalSampleFields() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        CustomerAccess customer = createAuthorizedCustomerContact(pilot);

        submitSampleForConfirmation(pilot, sampleId);
        String tokenResponse = mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm-token", sampleId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":" + customer.contactId() + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String confirmationToken = com.jayway.jsonpath.JsonPath.read(tokenResponse, "$.data.token");
        assertThat(count("SELECT COUNT(*) FROM nso_external_token WHERE tenant_id=1 AND token_hash=SHA2(?,256) AND legacy_confirmation_id IS NOT NULL", confirmationToken)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/public/confirm/{token}", confirmationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responsibleName").doesNotExist())
                .andExpect(jsonPath("$.data.issueSummary").doesNotExist())
                .andExpect(jsonPath("$.data.sampleNo").isString());
    }

    void sampleWorkflowCreatesAssignedTasksAndSupportsConditionalConfirmation() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);

        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type IN ('SAMPLE_PREPARE','SAMPLE_MAKE','INSPECTION','SAMPLE_CONFIRM')", pilot.projectId())).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND assignee_id IS NULL", pilot.projectId())).isZero();

        submitSampleForConfirmation(pilot, sampleId);
        CustomerAccess customer = createAuthorizedCustomerContact(pilot);
        String tokenResponse = mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm-token", sampleId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":" + customer.contactId() + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String confirmationToken = com.jayway.jsonpath.JsonPath.read(tokenResponse, "$.data.token");
        mockMvc.perform(post("/api/v1/public/confirm/{token}/decision", confirmationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusion\":\"CONDITIONAL_PASS\",\"opinion\":\"Proceed with documented conditions\",\"confirmer\":\"forged client value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONDITIONAL_PASS"))
                .andExpect(jsonPath("$.data.confirmConclusion").value("CONDITIONAL_PASS"));
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type='SAMPLE_CONFIRM' AND status='DONE'", pilot.projectId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT confirmer FROM nso_sample_confirm WHERE tenant_id=1 AND token=?", String.class, confirmationToken)).isEqualTo(customer.contactName());
    }

    void sampleWorkflowAssignsTasksFromModernProjectResponsibilities() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        jdbc.update("UPDATE nso_project_member SET project_role='PRODUCTION_MEMBER' WHERE tenant_id=1 AND project_id=? AND project_role='PRODUCTION'", pilot.projectId());
        jdbc.update("UPDATE nso_project_member SET project_role='QUALITY_MEMBER' WHERE tenant_id=1 AND project_id=? AND project_role='QUALITY'", pilot.projectId());
        jdbc.update("UPDATE nso_project_member_responsibility SET responsibility_code='PRODUCTION_MEMBER' WHERE tenant_id=1 AND project_id=? AND responsibility_code='PRODUCTION'", pilot.projectId());
        jdbc.update("UPDATE nso_project_member_responsibility SET responsibility_code='QUALITY_MEMBER' WHERE tenant_id=1 AND project_id=? AND responsibility_code='QUALITY'", pilot.projectId());

        long sampleId = createSample(pilot);

        assertThat(sampleId).isPositive();
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type IN ('SAMPLE_PREPARE','SAMPLE_MAKE','INSPECTION','SAMPLE_CONFIRM') AND assignee_id IS NOT NULL", pilot.projectId())).isEqualTo(4);
    }

    void v10MainLoopRunsFromReviewedRequirementToDeliveryAndArchive() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        submitSampleForConfirmation(pilot, sampleId);
        CustomerAccess customer = createAuthorizedCustomerContact(pilot);
        String tokenResponse = mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm-token", sampleId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":" + customer.contactId() + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String confirmationToken = com.jayway.jsonpath.JsonPath.read(tokenResponse, "$.data.token");
        mockMvc.perform(post("/api/v1/public/confirm/{token}/decision", confirmationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusion\":\"PASS\",\"opinion\":\"客户样品确认通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        projectAction(pilot, "PREPARE_PRODUCTION", "样品已确认，准备投产");
        projectAction(pilot, "START_PRODUCTION", "生产准备完成，开始投产");
        long productionTask = createTask(pilot, "PRODUCTION", "试点正式生产", pilot.versionNo(), "demo-production");
        completeTaskAs(productionTask, "demo-production");

        mockMvc.perform(get("/api/v1/admin/projects/{id}/delivery-readiness", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.blockers.length()").value(0));
        mockMvc.perform(post("/api/v1/admin/deliveries")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + pilot.projectId() + ",\"quantity\":2,\"logisticsNo\":\"SF-" + pilot.suffix() + "\",\"receiver\":\"客户收货人\",\"feedback\":\"签收前确认\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
        projectAction(pilot, "COMPLETE", "交付记录已创建，项目完成");
        archiveProject(pilot);

        assertThat(jdbc.queryForObject("SELECT status FROM nso_project WHERE tenant_id=1 AND id=?", String.class, pilot.projectId())).isEqualTo("ARCHIVED");
        assertEvent(pilot.projectId(), "DELIVERY_CREATED");
        assertEvent(pilot.projectId(), "PROJECT_ARCHIVE");
    }

    void v10BlocksManualProjectJumpsAndUnpreparedSampleSubmission() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        mockMvc.perform(post("/api/v1/admin/projects/{id}/actions/START_SAMPLING", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"manual\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.ruleCode").value("PROJECT_ACTION_SOURCE"));
        mockMvc.perform(post("/api/v1/admin/projects/{id}/actions/PENDING_DELIVERY", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"manual\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.ruleCode").value("PROJECT_ACTION_SOURCE"));

        long sampleId = createSample(pilot);
        mockMvc.perform(post("/api/v1/admin/samples/{id}/submit-confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.ruleCode").value("SAMPLE_QUALITY_CHECK"));
        mockMvc.perform(post("/api/v1/admin/samples/{id}/checks", sampleId)
                        .header("Authorization", bearer(loginToken("demo-quality", "admin123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkItem\":\"尺寸\",\"measuredValue\":\"10\",\"result\":\"PASS\",\"checkerName\":\"demo-quality\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/samples/{id}/submit-confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.ruleCode").value("SAMPLE_TASKS_PENDING"));
        completeTaskAs(taskId(pilot.projectId(), "SAMPLE_PREPARE"), "demo-production");
        completeTaskAs(taskId(pilot.projectId(), "SAMPLE_MAKE"), "demo-production");
        mockMvc.perform(post("/api/v1/admin/samples/{id}/submit-confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAIT_CUSTOMER_CONFIRM"));
        mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusion\":\"PASS\",\"opinion\":\"没有凭证\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.ruleCode").value("SAMPLE_PROXY_EVIDENCE"));
    }

    void v10DeliveryReadinessReportsBlocksAndPendingMilestonesHaveNoDate() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        mockMvc.perform(get("/api/v1/admin/projects/{id}/delivery-readiness", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.blockers[0].code").exists());
        jdbc.update("INSERT INTO nso_timeline_event (tenant_id,project_id,business_type,business_id,event_type,title,summary,operator_name) VALUES (1,?,'RISK',?,'EXECUTION_EXCEPTION_REPORTED','execution event','future event','test')", pilot.projectId(), pilot.projectId());
        mockMvc.perform(get("/api/v1/admin/projects/{id}/workspace", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.milestones[3].state").value("PENDING"))
                .andExpect(jsonPath("$.data.milestones[3].occurredAt").doesNotExist());
    }

    void fileAndTaskOperationsRequireProjectScopeAssigneeAndRole() throws Exception {
        BasicProject restrictedProject = createBasicProject();
        long restrictedFileId = createBoundFile(restrictedProject.projectId(), restrictedProject.suffix());
        mockMvc.perform(get("/api/v1/admin/files/{id}/download", restrictedFileId)
                        .header("Authorization", bearer(loginToken("demo-tech", "admin123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("PROJECT_DATA_SCOPE"));

        PilotProject pilot = createTechnicalPilot();
        long taskId = createTask(pilot, "PURCHASE", "assigned purchase task", pilot.versionNo(), "demo-purchase");

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-production", "admin123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("TASK_ASSIGNEE_SCOPE"));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-purchase", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    void qualityAssigneeCanStartAssignedInspectionTask() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long taskId = createTask(pilot, "INSPECTION", "assigned inspection task", pilot.versionNo(), "demo-quality");

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-quality", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    void projectManagerCanExecuteAssignedDeliveryTask() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        addProjectMember(pilot.projectId(), pilot.adminToken(), "demo-pm", "PROJECT_MANAGER");
        long taskId = createTask(pilot, "DELIVERY", "assigned delivery task", pilot.versionNo(), "demo-pm");
        String projectManagerToken = loginToken("demo-pm", "admin123");

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-production", "admin123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("TASK_ASSIGNEE_SCOPE"));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(projectManagerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assigneeId").value(userId("demo-pm")));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/feedback", taskId)
                        .header("Authorization", bearer(projectManagerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"DONE\",\"notes\":\"delivery task completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    void fileUploadRejectsMimeTypeMismatchBeforeObjectStorageAndGeneratorStaysWhitelisted() throws Exception {
        BasicProject project = createBasicProject();
        MockMultipartFile malformedDrawing = new MockMultipartFile("file", "drawing.dwg", "text/plain",
                "not-a-cad-file".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/files/upload")
                        .file(malformedDrawing)
                        .param("projectId", String.valueOf(project.projectId()))
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文件扩展名与 MIME 类型不匹配"));

        String token = loginToken();
        mockMvc.perform(post("/api/v1/admin/tool/gen/import").param("tableName", "sys_user")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("GEN_TABLE_DENIED"));
        mockMvc.perform(get("/api/v1/admin/tool/gen/99/preview").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("GEN_TABLE_DENIED"));
        byte[] zip = mockMvc.perform(get("/api/v1/admin/tool/gen/1/download").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(zip).startsWith((byte) 'P', (byte) 'K');
    }

    void quartzManualExecutionOnlyAcceptsWhitelistedJobs() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/v1/admin/monitor/jobs").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(7));
        mockMvc.perform(post("/api/v1/admin/monitor/jobs/99/run").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    void taskPlanningRequiresMatchingAssigneeResponsibilityAndBindsCurrentPublishedVersion() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        String invalidPayload = "{\"projectId\":" + pilot.projectId() + ",\"taskType\":\"INSPECTION\",\"title\":\"错误岗位检验任务\",\"referencedVersion\":\"V0\",\"assigneeId\":" + userId("demo-production") + "}";
        mockMvc.perform(post("/api/v1/admin/execution/tasks")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.ruleCode").value("TASK_ASSIGNEE_RESPONSIBILITY"));

        String validPayload = "{\"projectId\":" + pilot.projectId() + ",\"taskType\":\"INSPECTION\",\"title\":\"自动绑定版本检验任务\",\"referencedVersion\":\"V0\",\"assigneeId\":" + userId("demo-quality") + "}";
        String response = mockMvc.perform(post("/api/v1/admin/execution/tasks")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referencedVersion").value(pilot.versionNo()))
                .andReturn().getResponse().getContentAsString();
        long taskId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT referenced_doc_version_id FROM nso_task WHERE tenant_id=1 AND id=?", Long.class, taskId))
                .isEqualTo(pilot.documentId());
    }

    void ac01BlocksStaleDocumentVersionAndRecordsAudit() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long taskId = createTask(pilot, "PURCHASE", "过期版本采购任务", "V0");
        jdbc.update("UPDATE nso_task SET referenced_version='V0', referenced_doc_version_id=NULL WHERE tenant_id=1 AND id=?", taskId);

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.ruleCode").value("VERSION_MISMATCH"));

        assertEvent(pilot.projectId(), "TASK_CREATED");
        assertAudit("/api/v1/admin/tasks/" + taskId + "/start", "FAIL");
    }

    void ac02BlocksProductionUntilSampleConfirmed() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        submitSampleForConfirmation(pilot, sampleId);
        long taskId = createTask(pilot, "PRODUCTION", "待确认样品投产任务", pilot.versionNo());

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.ruleCode").value("SAMPLE_NOT_CONFIRMED"));

        mockMvc.perform(get("/api/v1/admin/tasks/{id}", taskId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TODO"));
        assertEvent(pilot.projectId(), "SAMPLE_SUBMITTED_CONFIRM");
        assertAudit("/api/v1/admin/tasks/" + taskId + "/start", "FAIL");
    }

    void ac03CreatesSixChangeImpactsApprovesAndBlocksAffectedTasks() throws Exception {
        ChangePilot pilot = createChangePilot();

        String impactResponse = mockMvc.perform(post("/api/v1/admin/changes/{id}/analyze-impact", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        List<String> impactTypes = com.jayway.jsonpath.JsonPath.read(impactResponse, "$.data[*].objectType");
        assertThat(new HashSet<>(impactTypes)).containsExactlyInAnyOrder(
                "DOCUMENT", "BOM", "INSPECTION", "SAMPLE", "PROCUREMENT", "PRODUCTION");

        mockMvc.perform(post("/api/v1/admin/changes/{id}/approve", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"opinion\":\"试点审批通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTING"));

        assertTaskStatus(pilot.project().adminToken(), pilot.purchaseTaskId(), "BLOCKED");
        assertTaskStatus(pilot.project().adminToken(), pilot.productionTaskId(), "BLOCKED");
        assertThat(count("SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=1 AND change_id=? AND decision='APPROVED'", pilot.changeId())).isEqualTo(4);
        assertEvent(pilot.project().projectId(), "CHANGE_APPROVED");
        assertAudit("/api/v1/admin/changes/" + pilot.changeId() + "/approve", "SUCCESS");
    }

    void ac04ClosesChangeAfterAllImpactsReceiveFeedback() throws Exception {
        ChangePilot pilot = createChangePilot();
        analyzeAndApprove(pilot);

        mockMvc.perform(post("/api/v1/admin/changes/{id}/feedback", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"DONE\",\"plan\":\"已完成现场换版\",\"delayDays\":1,\"reworkQty\":2,\"responsibleName\":\"试点负责人\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTING"));

        mockMvc.perform(post("/api/v1/admin/changes/{id}/close", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.delayDays").value(6))
                .andExpect(jsonPath("$.data.reworkQty").value(12));

        assertThat(count("SELECT COUNT(*) FROM nso_change_impact WHERE tenant_id=1 AND change_id=? AND status='DONE'", pilot.changeId())).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM nso_delay_rework WHERE tenant_id=1 AND change_id=?", pilot.changeId())).isEqualTo(6);
        assertEvent(pilot.project().projectId(), "CHANGE_CLOSED");
        assertAudit("/api/v1/admin/changes/" + pilot.changeId() + "/close", "SUCCESS");
    }

    void ac05ReportsExceptionAndProvidesExplainableRiskDetails() throws Exception {
        BasicProject pilot = createBasicProject();

        mockMvc.perform(post("/api/v1/admin/execution/exceptions")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + pilot.projectId() + ",\"exceptionType\":\"QUALITY\",\"summary\":\"试点装配尺寸异常\",\"reporterName\":\"现场质量\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("HIGH"))
                .andExpect(jsonPath("$.data.score").value(70));

        mockMvc.perform(get("/api/v1/admin/risks").param("projectId", String.valueOf(pilot.projectId()))
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].level").value("HIGH"))
                .andExpect(jsonPath("$.data.list[0].reasons[0]").value("现场异常:试点装配尺寸异常"));

        mockMvc.perform(get("/api/v1/admin/projects/{id}/risk", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("HIGH"))
                .andExpect(jsonPath("$.data.score").value(40))
                .andExpect(jsonPath("$.data.reasons[0]").value("距离交期仅 1 天"))
                .andExpect(jsonPath("$.data.suggestion").isString());
        assertEvent(pilot.projectId(), "EXECUTION_EXCEPTION_REPORTED");
    }

    void ac06DrillsOverviewIntoProjectTimelineAndBusinessDetails() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        submitSampleForConfirmation(pilot, sampleId);

        mockMvc.perform(get("/api/v1/admin/reports/overview").header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.projects").isNumber())
                .andExpect(jsonPath("$.data.metrics.waitingSamples").isNumber());
        mockMvc.perform(get("/api/v1/admin/projects/{id}", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project.id").value(pilot.projectId()))
                .andExpect(jsonPath("$.data.documents[0].versionNo").value(pilot.versionNo()))
                .andExpect(jsonPath("$.data.samples[0].status").value("WAIT_CUSTOMER_CONFIRM"))
                .andExpect(jsonPath("$.data.timeline[0].projectId").value(pilot.projectId()));
        assertEvent(pilot.projectId(), "SAMPLE_SUBMITTED_CONFIRM");
    }

    void dashboardAggregatesScopedDataAndValidatesPeriods() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        submitSampleForConfirmation(pilot, sampleId);
        createTask(pilot, "TECHNICAL", "仪表盘统计任务", pilot.versionNo());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .param("period", "30D")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.period").value("30D"))
                .andExpect(jsonPath("$.data.metrics.activeProjects").isNumber())
                .andExpect(jsonPath("$.data.metrics.pendingSamples").isNumber())
                .andExpect(jsonPath("$.data.deliveryTrend.length()").value(30))
                .andExpect(jsonPath("$.data.riskDistribution.length()").value(4))
                .andExpect(jsonPath("$.data.todos").isArray())
                .andExpect(jsonPath("$.data.shortcuts").isArray());

        String overviewResponse = mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .param("period", "30D")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.period").value("30D"))
                .andExpect(jsonPath("$.data.metrics.activeProjects").isNumber())
                .andExpect(jsonPath("$.data.metrics.pendingSamples").isNumber())
                .andExpect(jsonPath("$.data.shortcuts").isArray())
                .andReturn().getResponse().getContentAsString();
        String insightsResponse = mockMvc.perform(get("/api/v1/admin/dashboard/insights")
                        .param("period", "30D")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.period").value("30D"))
                .andExpect(jsonPath("$.data.deliveryTrend.length()").value(30))
                .andExpect(jsonPath("$.data.riskDistribution.length()").value(4))
                .andExpect(jsonPath("$.data.departmentLoads").isArray())
                .andReturn().getResponse().getContentAsString();
        String summaryResponse = mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .param("period", "30D")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").isNumber())
                .andExpect(jsonPath("$.data.deliveryTrend.length()").value(30))
                .andReturn().getResponse().getContentAsString();
        Object overviewActiveProjects = com.jayway.jsonpath.JsonPath.read(overviewResponse, "$.data.metrics.activeProjects");
        Object summaryActiveProjects = com.jayway.jsonpath.JsonPath.read(summaryResponse, "$.data.metrics.activeProjects");
        Object insightTrendLength = com.jayway.jsonpath.JsonPath.read(insightsResponse, "$.data.deliveryTrend.length()");
        Object summaryTrendLength = com.jayway.jsonpath.JsonPath.read(summaryResponse, "$.data.deliveryTrend.length()");
        assertThat(overviewActiveProjects).isEqualTo(summaryActiveProjects);
        assertThat(insightTrendLength).isEqualTo(summaryTrendLength);

        mockMvc.perform(get("/api/v1/admin/dashboard/projects")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(8))
                .andExpect(jsonPath("$.data.list").isArray());
        mockMvc.perform(get("/api/v1/admin/dashboard/actions/todos")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(8))
                .andExpect(jsonPath("$.data.list").isArray());
        mockMvc.perform(get("/api/v1/admin/dashboard/actions/risks")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(8))
                .andExpect(jsonPath("$.data.list").isArray());
        mockMvc.perform(get("/api/v1/admin/dashboard/actions/due")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(8))
                .andExpect(jsonPath("$.data.list").isArray());
        mockMvc.perform(get("/api/v1/admin/dashboard/actions/messages")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(8))
                .andExpect(jsonPath("$.data.list").isArray());

        mockMvc.perform(get("/api/v1/mp/dashboard")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .param("period", "CUSTOM")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .param("period", "CUSTOM")
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .param("period", "CUSTOM")
                        .param("startDate", LocalDate.now().minusDays(366).toString())
                        .param("endDate", LocalDate.now().toString())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest());
    }

    void dashboardDoesNotExposeNonMemberProjectMetrics() throws Exception {
        BasicProject project = createBasicProject();
        String username = "isolated-tech-" + Long.toUnsignedString(System.nanoTime());
        mockMvc.perform(post("/api/v1/admin/system/users")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"isolated-tech-password\",\"nickname\":\"Isolated Tech\",\"roleCodes\":[\"technical\"]}"))
                .andExpect(status().isOk());
        activateInternalUser(username, project.adminToken(), "activation-isolated-password", List.of("technical"));
        String technicalToken = completeForcedPasswordChange(username, "activation-isolated-password", "isolated-tech-password");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").value(0))
                .andExpect(jsonPath("$.data.projects.length()").value(0));

        mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").value(0))
                .andExpect(jsonPath("$.data.metrics.highRisks").value(0));
        mockMvc.perform(get("/api/v1/admin/dashboard/insights")
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskDistribution[0].value").value(0))
                .andExpect(jsonPath("$.data.departmentLoads").isArray());
        mockMvc.perform(get("/api/v1/admin/dashboard/actions/risks")
                        .param("pageNo", "1").param("pageSize", "8")
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(0));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").isNumber())
                .andExpect(jsonPath("$.data.projects").isArray());
    }

    void ac07DeniesProjectReadForNonMember() throws Exception {
        BasicProject pilot = createBasicProject();
        String technicalToken = loginToken("demo-tech", "admin123");

        mockMvc.perform(get("/api/v1/admin/projects/{id}", pilot.projectId())
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data.ruleCode").value("PROJECT_DATA_SCOPE"));

        assertAudit("/api/v1/admin/projects/" + pilot.projectId(), "FAIL");
    }

    void ac08RejectsIdempotentReplayAndStaleTaskVersion() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        String requestId = "pilot-task-" + System.nanoTime();
        String taskPayload = "{\"projectId\":" + pilot.projectId() + ",\"taskType\":\"PURCHASE\",\"title\":\"幂等采购任务\",\"referencedVersion\":\"" + pilot.versionNo() + "\",\"responsibleName\":\"采购员\",\"assigneeId\":" + userId("demo-purchase") + "}";
        String taskResponse = mockMvc.perform(post("/api/v1/admin/execution/tasks")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long taskId = ((Number) com.jayway.jsonpath.JsonPath.read(taskResponse, "$.data.id")).longValue();

        mockMvc.perform(post("/api/v1/admin/execution/tasks")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.ruleCode").value("IDEMPOTENT_REPLAY"));

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .header("X-Request-Id", "pilot-start-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.version").value(1));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/pause", taskId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .header("X-Request-Id", "pilot-pause-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"旧版本提交\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.ruleCode").value("OPTIMISTIC_LOCK"));

        assertTaskStatus(pilot.adminToken(), taskId, "IN_PROGRESS");
        assertEvent(pilot.projectId(), "TASK_STARTED");
    }

    void actionCenterProjectionDeduplicatesAndResolvesFromTaskFact() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long taskId = createTask(pilot, "COORDINATION", "统一行动投影任务", pilot.versionNo(), "api-admin");
        String token = pilot.adminToken();

        mockMvc.perform(post("/api/v1/admin/actions/refresh")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshed").isNumber());

        String first = mockMvc.perform(get("/api/v1/admin/actions")
                        .param("pageSize", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/v1/admin/actions")
                        .param("pageSize", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<?> firstRows = com.jayway.jsonpath.JsonPath.read(first, "$.data.list[?(@.sourceType=='TASK' && @.sourceId==" + taskId + ")]");
        List<?> secondRows = com.jayway.jsonpath.JsonPath.read(second, "$.data.list[?(@.sourceType=='TASK' && @.sourceId==" + taskId + ")]");
        assertThat(firstRows).hasSize(1);
        assertThat(secondRows).hasSize(1);
        assertThat(count("SELECT COUNT(*) FROM nso_action_item WHERE tenant_id=1 AND source_type='TASK' AND source_id=? AND action_code='EXECUTE'", taskId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/projects/{id}/workspace/pulse", pilot.projectId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(pilot.projectId()))
                .andExpect(jsonPath("$.data.blockers").isArray())
                .andExpect(jsonPath("$.data.metrics").isMap());

        completeTaskAs(taskId, "api-admin");
        mockMvc.perform(post("/api/v1/admin/actions/refresh")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        String resolved = mockMvc.perform(get("/api/v1/admin/actions")
                        .param("pageSize", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<?> resolvedRows = com.jayway.jsonpath.JsonPath.read(resolved, "$.data.list[?(@.sourceType=='TASK' && @.sourceId==" + taskId + ")]");
        assertThat(resolvedRows).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM nso_action_item WHERE tenant_id=1 AND source_type='TASK' AND source_id=? AND action_status='RESOLVED'", taskId)).isEqualTo(1);
    }

    void capaEnforcesStateEvidenceActionsAndSerialCloseApproval() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        String token = pilot.adminToken();
        LocalDateTime dueAt = LocalDateTime.now().plusDays(2).withNano(0);
        String created = mockMvc.perform(post("/api/v1/admin/exceptions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + pilot.projectId() + ",\"exceptionType\":\"QUALITY_EXCEPTION\",\"summary\":\"现场尺寸异常需要闭环\",\"ownerUserId\":" + userId("api-admin") + ",\"dueAt\":\"" + dueAt + "\",\"idempotencyKey\":\"capa-test-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long caseId = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.data.id")).longValue();

        mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"ACTIONING\",\"version\":0,\"idempotencyKey\":\"illegal-" + pilot.suffix() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.ruleCode").value("CAPA_STATUS"));

        String contained = mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"CONTAINED\",\"comment\":\"隔离异常批次并暂停放行\",\"version\":0,\"idempotencyKey\":\"contain-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTAINED"))
                .andReturn().getResponse().getContentAsString();
        int containedVersion = ((Number) com.jayway.jsonpath.JsonPath.read(contained, "$.data.version")).intValue();

        mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"CONTAINED\",\"comment\":\"隔离异常批次并暂停放行\",\"version\":0,\"idempotencyKey\":\"contain-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTAINED"));
        assertThat(count("SELECT COUNT(*) FROM nso_capa_transition WHERE tenant_id=1 AND exception_case_id=?", caseId)).isEqualTo(1);

        String analyzing = mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"ANALYZING\",\"version\":" + containedVersion + ",\"idempotencyKey\":\"analyze-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANALYZING"))
                .andReturn().getResponse().getContentAsString();
        int analyzingVersion = ((Number) com.jayway.jsonpath.JsonPath.read(analyzing, "$.data.version")).intValue();

        String actioning = mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"ACTIONING\",\"rootCause\":\"夹具定位面磨损\",\"correctivePlan\":\"更换定位件并复测首件\",\"version\":" + analyzingVersion + ",\"idempotencyKey\":\"action-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIONING"))
                .andReturn().getResponse().getContentAsString();
        int actioningVersion = ((Number) com.jayway.jsonpath.JsonPath.read(actioning, "$.data.version")).intValue();

        String action = mockMvc.perform(post("/api/v1/admin/exceptions/{id}/actions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更换定位件并完成首件复测\",\"assigneeUserId\":" + userId("api-admin") + ",\"planStart\":\"" + LocalDate.now() + "\",\"planFinish\":\"" + LocalDate.now().plusDays(1) + "\",\"actionPlan\":\"更换后按检验规范复测\",\"idempotencyKey\":\"action-task-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();
        long correctiveTaskId = ((Number) com.jayway.jsonpath.JsonPath.read(action, "$.data.sourceId")).longValue();
        completeTaskAs(correctiveTaskId, "api-admin");

        mockMvc.perform(post("/api/v1/admin/exceptions/{id}/evidence", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceType\":\"CHECK\",\"evidenceRef\":\"QC-" + pilot.suffix() + "\",\"summary\":\"首件复测合格\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidenceRef").value("QC-" + pilot.suffix()));

        String verifying = mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"VERIFYING\",\"verificationSummary\":\"首件尺寸和外观复测通过\",\"version\":" + actioningVersion + ",\"idempotencyKey\":\"verify-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFYING"))
                .andReturn().getResponse().getContentAsString();
        int verifyingVersion = ((Number) com.jayway.jsonpath.JsonPath.read(verifying, "$.data.version")).intValue();

        String todos = mockMvc.perform(get("/api/v1/admin/approval-todos")
                        .param("businessType", "EXCEPTION")
                        .param("pageSize", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> todoRows = com.jayway.jsonpath.JsonPath.read(todos, "$.data.list");
        Map<String, Object> firstTodo = todoRows.stream()
                .filter(row -> ((Number) row.get("businessId")).longValue() == caseId)
                .findFirst().orElseThrow();
        long firstTodoId = ((Number) firstTodo.get("id")).longValue();
        int firstTodoVersion = ((Number) firstTodo.get("version")).intValue();
        mockMvc.perform(post("/api/v1/admin/approval-todos/{id}/decision", firstTodoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"version\":" + firstTodoVersion + ",\"idempotencyKey\":\"approve-pm-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk());
        String secondTodoResponse = mockMvc.perform(get("/api/v1/admin/approval-todos")
                        .param("businessType", "EXCEPTION")
                        .param("pageSize", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> secondTodoRows = com.jayway.jsonpath.JsonPath.read(secondTodoResponse, "$.data.list");
        Map<String, Object> secondTodo = secondTodoRows.stream()
                .filter(row -> ((Number) row.get("businessId")).longValue() == caseId)
                .findFirst().orElseThrow();
        long secondTodoId = ((Number) secondTodo.get("id")).longValue();
        int secondTodoVersion = ((Number) secondTodo.get("version")).intValue();
        mockMvc.perform(post("/api/v1/admin/approval-todos/{id}/decision", secondTodoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"version\":" + secondTodoVersion + ",\"idempotencyKey\":\"approve-quality-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/exceptions/{id}/transitions", caseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"CLOSED\",\"closeConclusion\":\"整改完成，验证证据已归档\",\"version\":" + verifyingVersion + ",\"idempotencyKey\":\"close-" + pilot.suffix() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        assertThat(count("SELECT COUNT(*) FROM nso_business_event WHERE tenant_id=1 AND project_id=? AND action_code='CAPA_CLOSED'", pilot.projectId())).isGreaterThan(0);
    }

    void approvalTemplatesAreVersionedAndProjectPulseExposesDecisionSignals() throws Exception {
        BasicProject project = createBasicProject();
        String token = project.adminToken();
        String code = "TEST_TEMPLATE_" + project.suffix();
        String draft = mockMvc.perform(post("/api/v1/admin/approval-templates")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateCode\":\"" + code + "\",\"templateName\":\"试点变更模板\",\"businessType\":\"CHANGE\",\"approvalMode\":\"COUNTERSIGN\",\"slaMinutes\":120,\"nodes\":[{\"nodeCode\":\"PM\",\"nodeName\":\"项目经理\",\"responsibilityCode\":\"PROJECT_MANAGER\"},{\"nodeCode\":\"QUALITY\",\"nodeName\":\"质量\",\"responsibilityCode\":\"QUALITY\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateVersion").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long templateId = ((Number) com.jayway.jsonpath.JsonPath.read(draft, "$.data.id")).longValue();
        try {
            mockMvc.perform(post("/api/v1/admin/approval-templates/{id}/publish", templateId)
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
            String templateListResponse = mockMvc.perform(get("/api/v1/admin/approval-templates")
                            .param("businessType", "CHANGE")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Map<String, Object>> templateRows = com.jayway.jsonpath.JsonPath.read(templateListResponse, "$.data.list");
            Map<String, Object> templateRow = templateRows.stream()
                    .filter(row -> code.equals(row.get("templateCode")))
                    .findFirst().orElseThrow();
            assertThat(templateRow.get("approvalMode")).isEqualTo("COUNTERSIGN");
            assertThat((List<?>) templateRow.get("nodes")).hasSize(2);

            ChangePilot compatibilityPilot = createChangePilot();
            analyzeAndApprove(compatibilityPilot);
            assertThat(count("SELECT COUNT(*) FROM nso_approval_task WHERE tenant_id=1 AND instance_id=(SELECT id FROM nso_approval_instance WHERE tenant_id=1 AND business_type='CHANGE' AND business_id=?) AND node_code IN ('PM','QUALITY')", compatibilityPilot.changeId())).isEqualTo(2);

            mockMvc.perform(get("/api/v1/admin/projects/{id}/workspace/pulse", project.projectId())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stage").isString())
                    .andExpect(jsonPath("$.data.pendingDecisions").isArray())
                    .andExpect(jsonPath("$.data.openExceptions").isArray());
        } finally {
            jdbc.update("UPDATE nso_approval_template SET status='RETIRED' WHERE tenant_id=? AND id=?", 1L, templateId);
            jdbc.update("UPDATE nso_approval_template SET status='PUBLISHED' WHERE tenant_id=? AND template_code='CHANGE_STANDARD' AND template_version=1", 1L);
        }
    }

    private ChangePilot createChangePilot() throws Exception {
        PilotProject project = createTechnicalPilot();
        createSample(project);
        long purchaseTaskId = createTask(project, "PURCHASE", "变更物料核对", project.versionNo());
        long productionTaskId = createTask(project, "PRODUCTION", "变更后生产排程", project.versionNo());
        String response = mockMvc.perform(post("/api/v1/admin/changes")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + project.projectId() + ",\"changeType\":\"DRAWING\",\"urgency\":\"NORMAL\",\"beforeContent\":\"孔位旧尺寸\",\"afterContent\":\"孔位新尺寸\",\"reason\":\"客户确认变更\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAIT_IMPACT"))
                .andReturn().getResponse().getContentAsString();
        long changeId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
        return new ChangePilot(project, changeId, purchaseTaskId, productionTaskId);
    }

    private void analyzeAndApprove(ChangePilot pilot) throws Exception {
        mockMvc.perform(post("/api/v1/admin/changes/{id}/analyze-impact", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));
        mockMvc.perform(post("/api/v1/admin/changes/{id}/approve", pilot.changeId())
                        .header("Authorization", bearer(pilot.project().adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTING"));
    }

    private PilotProject createTechnicalPilot() throws Exception {
        BasicProject project = createBasicProject();
        mockMvc.perform(post("/api/v1/admin/projects/{id}/submit-review", project.projectId())
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWING"));
        mockMvc.perform(post("/api/v1/admin/projects/{id}/actions/APPROVE_REVIEW", project.projectId())
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"试点需求评审通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TECH_PREPARING"));
        String technicalUsername = "demo-tech";
        addProjectMember(project, technicalUsername, "TECHNICAL");
        addProjectMember(project, "demo-purchase", "PURCHASER");
        addProjectMember(project, "demo-production", "PRODUCTION");
        addProjectMember(project, "demo-quality", "QUALITY");
        String technicalToken = loginToken(technicalUsername, "admin123");
        String versionNo = "V1-" + project.suffix();
        long fileObjectId = createBoundFile(project.projectId(), project.suffix());
        String documentResponse = mockMvc.perform(post("/api/v1/admin/documents/{projectId}/versions", project.projectId())
                        .header("Authorization", bearer(technicalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"试点图纸-" + project.suffix() + ".pdf\",\"fileType\":\"DRAWING\",\"versionNo\":\"" + versionNo + "\",\"fileObjectId\":" + fileObjectId + ",\"effectiveDate\":\"2030-01-01\",\"changeSummary\":\"试点首版\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long documentId = ((Number) com.jayway.jsonpath.JsonPath.read(documentResponse, "$.data.id")).longValue();

        mockMvc.perform(post("/api/v1/admin/document-versions/{id}/publish", documentId)
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.currentVersion").value(true));

        mockMvc.perform(post("/api/v1/admin/boms")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + project.projectId() + ",\"bomNo\":\"BOM-" + project.suffix() + "\",\"versionNo\":\"" + versionNo + "\",\"boundDocVersionId\":" + documentId + ",\"items\":[{\"materialCode\":\"MAT-01\",\"materialName\":\"定位销\",\"quantity\":2,\"unit\":\"EA\"}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/process-routes")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + project.projectId() + ",\"routeNo\":\"ROUTE-" + project.suffix() + "\",\"versionNo\":\"" + versionNo + "\",\"boundDocVersionId\":" + documentId + ",\"steps\":[{\"stepNo\":10,\"stepName\":\"机加工\",\"standardHours\":1.5}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/inspection-specs")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + project.projectId() + ",\"specNo\":\"INS-" + project.suffix() + "\",\"versionNo\":\"" + versionNo + "\",\"boundDocVersionId\":" + documentId + ",\"items\":[{\"itemName\":\"关键尺寸\",\"standardValue\":\"±0.1mm\",\"samplingRule\":\"全检\"}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/projects/{id}/technical-package/publish", project.projectId())
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TECH_PUBLISHED"));

        return new PilotProject(project.projectId(), project.adminToken(), project.suffix(), documentId, versionNo);
    }

    private BasicProject createBasicProject() throws Exception {
        String token = loginToken();
        String suffix = Long.toUnsignedString(System.nanoTime());
        String customerResponse = mockMvc.perform(post("/api/v1/admin/customers")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"试点客户-" + suffix + "\",\"industry\":\"装备制造\",\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long customerId = ((Number) com.jayway.jsonpath.JsonPath.read(customerResponse, "$.data.id")).longValue();
        String projectResponse = mockMvc.perform(post("/api/v1/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"productName\":\"试点工装-" + suffix + "\",\"quantity\":2,\"targetDate\":\"" + LocalDate.now().plusDays(1) + "\",\"priority\":\"HIGH\",\"requirements\":[\"定位尺寸公差\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long projectId = ((Number) com.jayway.jsonpath.JsonPath.read(projectResponse, "$.data.id")).longValue();
        return new BasicProject(projectId, token, suffix);
    }

    private void addProjectMember(BasicProject project, String username, String projectRole) throws Exception {
        addProjectMember(project.projectId(), project.adminToken(), username, projectRole);
    }

    private void addProjectMember(long projectId, String adminToken, String username, String projectRole) throws Exception {
        long memberUserId = userId(username);
        mockMvc.perform(post("/api/v1/admin/projects/{id}/members", projectId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberUserId + ",\"memberName\":\"" + username + "\",\"projectRole\":\"" + projectRole + "\",\"departmentName\":\"试点组\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(memberUserId))
                .andExpect(jsonPath("$.data.memberName").isNotEmpty())
                .andExpect(jsonPath("$.data.primaryResponsibilityCode").value(projectRole));
    }

    private CustomerAccess createAuthorizedCustomerContact(PilotProject pilot) throws Exception {
        Long customerId = jdbc.queryForObject("SELECT customer_id FROM nso_project WHERE tenant_id=1 AND id=?", Long.class, pilot.projectId());
        long beforeUsers = count("SELECT COUNT(*) FROM sys_user WHERE tenant_id=1");
        String contactName = "External Contact " + pilot.suffix();
        String contactResponse = mockMvc.perform(post("/api/v1/admin/customers/{customerId}/contacts", customerId)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactName\":\"" + contactName + "\",\"phone\":\"13800001234\",\"email\":\"" + pilot.suffix() + "@example.test\",\"positionName\":\"Customer QA\",\"preferredChannel\":\"LINK\",\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long contactId = ((Number) com.jayway.jsonpath.JsonPath.read(contactResponse, "$.data.id")).longValue();
        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/customer-authorizations", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":" + contactId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactId").value(contactId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(count("SELECT COUNT(*) FROM sys_user WHERE tenant_id=1")).isEqualTo(beforeUsers);
        return new CustomerAccess(contactId, contactName);
    }

    private void activateInternalUser(String username, String adminToken, String password, List<String> roleCodes) throws Exception {
        String rolesJson = roleCodes.stream().map(role -> "\"" + role + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(post("/api/v1/admin/system/users/{userId}/activate", userId(username))
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + password + "\",\"roleCodes\":[" + rolesJson + "],\"reason\":\"integration test activation\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM sys_user WHERE tenant_id=1 AND username=?", String.class, username)).isEqualTo("ACTIVE");
    }

    private String completeForcedPasswordChange(String username, String temporaryPassword, String finalPassword) throws Exception {
        String activationToken = loginToken(username, temporaryPassword);
        mockMvc.perform(put("/api/v1/admin/profile/password")
                        .header("Authorization", bearer(activationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + temporaryPassword + "\",\"newPassword\":\"" + finalPassword + "\"}"))
                .andExpect(status().isOk());
        return loginToken(username, finalPassword);
    }

    private long createSample(PilotProject pilot) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/samples")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + pilot.projectId() + ",\"purpose\":\"试点首件确认\",\"quantity\":1,\"planFinishDate\":\"" + LocalDate.now().plusDays(1) + "\",\"referencedVersion\":\"" + pilot.versionNo() + "\",\"responsibleName\":\"试点工程师\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    private void projectAction(PilotProject pilot, String action, String reason) throws Exception {
        mockMvc.perform(post("/api/v1/admin/projects/{id}/actions/{action}", pilot.projectId(), action)
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private void archiveProject(PilotProject pilot) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/operation-confirmations")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationCode\":\"PROJECT_ARCHIVE\",\"businessType\":\"PROJECT\",\"businessId\":" + pilot.projectId() + ",\"reason\":\"验收归档\",\"channel\":\"IN_APP\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long confirmationId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
        mockMvc.perform(post("/api/v1/admin/operation-confirmations/{id}/confirm", confirmationId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/projects/{id}/actions/ARCHIVE", pilot.projectId())
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"验收归档\",\"confirmationId\":" + confirmationId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    private void submitSampleForConfirmation(PilotProject pilot, long sampleId) throws Exception {
        completeSamplePrerequisites(pilot, sampleId);
        mockMvc.perform(post("/api/v1/admin/samples/{id}/submit-confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAIT_CUSTOMER_CONFIRM"));
    }

    private void completeSamplePrerequisites(PilotProject pilot, long sampleId) throws Exception {
        completeTaskAs(taskId(pilot.projectId(), "SAMPLE_PREPARE"), "demo-production");
        completeTaskAs(taskId(pilot.projectId(), "SAMPLE_MAKE"), "demo-production");
        completeTaskAs(taskId(pilot.projectId(), "INSPECTION"), "demo-quality");
        mockMvc.perform(post("/api/v1/admin/samples/{id}/checks", sampleId)
                        .header("Authorization", bearer(loginToken("demo-quality", "admin123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkItem\":\"试点尺寸\",\"measuredValue\":\"10.00\",\"result\":\"PASS\",\"checkerName\":\"demo-quality\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PASS"));
    }

    private long taskId(long projectId, String taskType) {
        Long id = jdbc.queryForObject("SELECT id FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type=? ORDER BY id DESC LIMIT 1", Long.class, projectId, taskType);
        if (id == null) throw new IllegalStateException("Missing " + taskType + " task");
        return id;
    }

    private void completeTaskAs(long taskId, String username) throws Exception {
        // 集成测试夹具通过多条项目成员兼容路径创建任务分配。
        // 此处使用管理员的显式监督权限；上方的专门测试仍会验证
        // 非受分配人会被拒绝，且普通用户仍受受分配角色校验约束。
        String token = loginToken();
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/complete", taskId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    private record CustomerAccess(long contactId, String contactName) { }

    private long createTask(PilotProject pilot, String taskType, String title, String referencedVersion) throws Exception {
        return createTask(pilot, taskType, title, referencedVersion, defaultTaskAssignee(taskType));
    }

    private String defaultTaskAssignee(String taskType) {
        return switch (taskType) {
            case "PURCHASE" -> "demo-purchase";
            case "PRODUCTION", "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK" -> "demo-production";
            case "QUALITY", "INSPECTION" -> "demo-quality";
            case "TECHNICAL", "DESIGN" -> "demo-tech";
            default -> "api-admin";
        };
    }

    private long createTask(PilotProject pilot, String taskType, String title, String referencedVersion, String assigneeUsername) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/execution/tasks")
                        .header("Authorization", bearer(pilot.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + pilot.projectId() + ",\"taskType\":\"" + taskType + "\",\"title\":\"" + title + "\",\"referencedVersion\":\"" + referencedVersion + "\",\"responsibleName\":\"试点负责人\",\"assigneeId\":" + userId(assigneeUsername) + ",\"planStart\":\"" + LocalDate.now() + "\",\"planFinish\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    private long createBoundFile(long projectId, String suffix) {
        String storagePath = "tests/" + suffix + ".pdf";
        jdbc.update("INSERT INTO nso_file_object (file_name, content_type, file_size, sha256, storage_path, tenant_id, project_id, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "pilot-" + suffix + ".pdf", "application/pdf", 1L, "sha-" + suffix, storagePath, 1L, projectId, userId("api-admin"));
        Long id = jdbc.queryForObject("SELECT id FROM nso_file_object WHERE tenant_id=1 AND project_id=? AND storage_path=?", Long.class, projectId, storagePath);
        if (id == null) {
            throw new IllegalStateException("Failed to create project-bound file");
        }
        return id;
    }

    private long userId(String username) {
        Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE tenant_id=1 AND username=?", Long.class, username);
        if (id == null) {
            throw new IllegalStateException("Missing test user: " + username);
        }
        return id;
    }

    private void assertTaskStatus(String token, long taskId, String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/v1/admin/tasks/{id}", taskId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(expectedStatus));
    }

    private void assertEvent(long projectId, String action) {
        assertThat(count("SELECT COUNT(*) FROM nso_business_event WHERE tenant_id=1 AND project_id=? AND action_code=?", projectId, action)).isGreaterThan(0);
    }

    private void assertAudit(String uri, String result) {
        assertThat(count("SELECT COUNT(*) FROM nso_audit_log WHERE tenant_id=1 AND request_uri=? AND result=?", uri, result)).isGreaterThan(0);
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbc.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginToken() throws Exception {
        return loginToken("api-admin", "api-admin-password");
    }

    private String loginToken(String username, String password) throws Exception {
        return loginTokens(username, password).accessToken();
    }

    private LoginTokens loginTokens(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn().getResponse().getContentAsString();
        return new LoginTokens(com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken"),
                com.jayway.jsonpath.JsonPath.read(response, "$.data.refreshToken"));
    }

    private record LoginTokens(String accessToken, String refreshToken) { }

    private record BasicProject(long projectId, String adminToken, String suffix) { }

    private record PilotProject(long projectId, String adminToken, String suffix, long documentId, String versionNo) { }

    private record ChangePilot(PilotProject project, long changeId, long purchaseTaskId, long productionTaskId) { }
}
