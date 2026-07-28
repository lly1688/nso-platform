package com.nso;

import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.framework.security.RequestRateLimitService;
import com.nso.framework.security.NsoDemoScenarioInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
        "nso.demo.default-password=demo-password-123"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class NsoApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CustomerMapper customers;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestRateLimitService requestRateLimitService;

    @Autowired
    private NsoDemoScenarioInitializer demoScenarios;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("nso_platform")
            .withUsername("nso_app")
            .withPassword("nso_app_change_me");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("nso.minio.endpoint", () -> "http://127.0.0.1:9000");
        registry.add("nso.minio.access-key", () -> "test-access-key");
        registry.add("nso.minio.secret-key", () -> "test-secret-key");
    }

    @Test
    void contextLoads() {
    }

    /**
     * V1 data must be able to upgrade through the immutable V1-V6 chain and
     * receive the V7 governance backfill.  This runs against a clean MySQL
     * container, which is the same path used by an existing V1 database.
     */
    @Test
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
        Integer demoUserCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username LIKE 'demo-%'", Integer.class);

        assertThat(latestVersion).isEqualTo(16);
        assertThat(defaultTenants).isEqualTo(1);
        assertThat(governedUsers).isEqualTo(1);
        assertThat(governedFiles).isEqualTo(1);
        assertThat(governedProjectFiles).isEqualTo(1);
        assertThat(departments).isGreaterThan(0);
        assertThat(menus).isGreaterThan(0);
        assertThat(roleCount).isGreaterThanOrEqualTo(9);
        assertThat(roleMenuCount).isGreaterThan(0);
        assertThat(demoUserCount).isEqualTo(10);

        jdbc.update("INSERT INTO nso_idempotency_key (tenant_id, user_id, request_id, request_method, request_path, status) VALUES (1, 9, 'upgrade-test-request', 'POST', '/api/v1/admin/projects', 'COMPLETE')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO nso_idempotency_key (tenant_id, user_id, request_id, request_method, request_path, status) VALUES (1, 9, 'upgrade-test-request', 'POST', '/api/v1/admin/projects', 'COMPLETE')"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
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

    @Test
    void protectedApiReturnsStructuredUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期或尚未登录"));
    }

    @Test
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

    @Test
    void rateLimiterRejectsRequestsAfterTheConfiguredWindowQuota() {
        String scope = "test-" + System.nanoTime();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(requestRateLimitService.tryAcquire(scope, "pilot-user", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
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

    @Test
    void administratorCanMaintainOrganizationRolesMenusAndUsers() throws Exception {
        String token = loginToken();
        String suffix = String.valueOf(System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/system/departments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].deptName").value("默认部门"));

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
                .andExpect(jsonPath("$.data[0].dict_code").value("code_" + suffix));

        mockMvc.perform(post("/api/v1/admin/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"designer" + suffix + "\",\"password\":\"initial-password\",\"nickname\":\"测试用户\",\"roleCodes\":[\"" + roleCode + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("designer" + suffix));

        mockMvc.perform(get("/api/v1/admin/system/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
    }

    @Test
    void demoRolesExposeOnlyTheirGrantedFunctionsAndPermissionChangesRevokeOldTokens() throws Exception {
        LoginTokens executiveSession = loginTokens("demo-executive", "demo-password-123");
        String executiveToken = executiveSession.accessToken();
        mockMvc.perform(get("/api/v1/admin/reports/overview").header("Authorization", "Bearer " + executiveToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/admin/system/users").header("Authorization", "Bearer " + executiveToken))
                .andExpect(status().isForbidden());

        String customerToken = loginToken("demo-customer", "demo-password-123");
        mockMvc.perform(get("/api/v1/admin/projects").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/mp/projects").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/mp/customer/projects").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deniedAudits = jdbc.queryForObject("SELECT COUNT(*) FROM nso_audit_log WHERE user_name='demo-executive' AND operation_type='ACCESS_DENIED' AND result='DENIED'", Integer.class);
        Integer sensitiveAuditValues = jdbc.queryForObject("SELECT COUNT(*) FROM nso_audit_log WHERE after_summary LIKE '%demo-password-123%' OR after_summary LIKE '%eyJ%'", Integer.class);
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

    @Test
    void fieldRoleAndAdministrativeToolsHonorTheV12PermissionBoundary() throws Exception {
        for (String username : List.of("demo-admin", "demo-pm", "demo-tech", "demo-process", "demo-purchase", "demo-production", "demo-quality", "demo-field", "demo-customer", "demo-executive")) {
            mockMvc.perform(get("/api/v1/admin/auth/me").header("Authorization", bearer(loginToken(username, "demo-password-123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value(username));
        }

        String fieldToken = loginToken("demo-field", "demo-password-123");
        mockMvc.perform(get("/api/v1/admin/projects").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/monitor/jobs").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/tool/gen/tables").header("Authorization", bearer(fieldToken)))
                .andExpect(status().isForbidden());
    }

    @Test
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

        String token = loginToken(username, oldPassword);
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

    @Test
    void demoScenarioInitializerCanRunAgainWithoutDuplicateRoleFixtures() throws Exception {
        int beforeMembers = count("SELECT COUNT(*) FROM nso_project_member WHERE tenant_id=1 AND project_id IN (SELECT id FROM nso_project WHERE tenant_id=1 AND project_no LIKE 'NSO-DEMO-%')");
        int beforeTasks = count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND task_no LIKE 'TSK-DEMO-%'");
        int beforeMessages = count("SELECT COUNT(*) FROM nso_message WHERE tenant_id=1 AND title LIKE '%待%'");
        demoScenarios.run(new DefaultApplicationArguments(new String[0]));
        assertThat(count("SELECT COUNT(*) FROM nso_project_member WHERE tenant_id=1 AND project_id IN (SELECT id FROM nso_project WHERE tenant_id=1 AND project_no LIKE 'NSO-DEMO-%')")).isEqualTo(beforeMembers);
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND task_no LIKE 'TSK-DEMO-%'")).isEqualTo(beforeTasks);
        assertThat(count("SELECT COUNT(*) FROM nso_message WHERE tenant_id=1 AND title LIKE '%待%'")).isEqualTo(beforeMessages);
    }

    @Test
    void customerAndPublicConfirmationEndpointsRedactInternalSampleFields() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);
        addProjectMember(pilot.projectId(), pilot.adminToken(), "demo-customer", "CUSTOMER_CONFIRM");

        String customerToken = loginToken("demo-customer", "demo-password-123");
        mockMvc.perform(get("/api/v1/mp/customer/projects/{id}/samples", pilot.projectId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].responsibleName").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].issueSummary").doesNotExist());

        submitSampleForConfirmation(pilot, sampleId);
        String tokenResponse = mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm-token", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String confirmationToken = com.jayway.jsonpath.JsonPath.read(tokenResponse, "$.data.token");
        mockMvc.perform(get("/api/v1/public/confirm/{token}", confirmationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responsibleName").doesNotExist())
                .andExpect(jsonPath("$.data.issueSummary").doesNotExist())
                .andExpect(jsonPath("$.data.sampleNo").isString());
    }

    @Test
    void sampleWorkflowCreatesAssignedTasksAndSupportsConditionalConfirmation() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long sampleId = createSample(pilot);

        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type IN ('SAMPLE_PREPARE','SAMPLE_MAKE','INSPECTION','SAMPLE_CONFIRM')", pilot.projectId())).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND assignee_id IS NULL", pilot.projectId())).isZero();

        submitSampleForConfirmation(pilot, sampleId);
        String tokenResponse = mockMvc.perform(post("/api/v1/admin/samples/{id}/confirm-token", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String confirmationToken = com.jayway.jsonpath.JsonPath.read(tokenResponse, "$.data.token");
        mockMvc.perform(post("/api/v1/public/confirm/{token}/decision", confirmationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusion\":\"CONDITIONAL_PASS\",\"opinion\":\"Proceed with documented conditions\",\"confirmer\":\"Customer QA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONDITIONAL_PASS"))
                .andExpect(jsonPath("$.data.confirmConclusion").value("CONDITIONAL_PASS"));
        assertThat(count("SELECT COUNT(*) FROM nso_task WHERE tenant_id=1 AND project_id=? AND task_type='SAMPLE_CONFIRM' AND status='DONE'", pilot.projectId())).isEqualTo(1);
    }

    @Test
    void fileAndTaskOperationsRequireProjectScopeAssigneeAndRole() throws Exception {
        BasicProject restrictedProject = createBasicProject();
        long restrictedFileId = createBoundFile(restrictedProject.projectId(), restrictedProject.suffix());
        mockMvc.perform(get("/api/v1/admin/files/{id}/download", restrictedFileId)
                        .header("Authorization", bearer(loginToken("demo-tech", "demo-password-123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("PROJECT_DATA_SCOPE"));

        PilotProject pilot = createTechnicalPilot();
        addProjectMember(pilot.projectId(), pilot.adminToken(), "demo-purchase", "PURCHASER");
        long taskId = createTask(pilot, "PURCHASE", "assigned purchase task", pilot.versionNo(), "demo-purchase");

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-production", "demo-password-123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.ruleCode").value("TASK_ASSIGNEE_SCOPE"));
        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(loginToken("demo-purchase", "demo-password-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
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

    @Test
    void quartzManualExecutionOnlyAcceptsWhitelistedJobs() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/v1/admin/monitor/jobs").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6));
        mockMvc.perform(post("/api/v1/admin/monitor/jobs/99/run").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ac01BlocksStaleDocumentVersionAndRecordsAudit() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        long taskId = createTask(pilot, "PURCHASE", "过期版本采购任务", "V0");

        mockMvc.perform(post("/api/v1/admin/tasks/{id}/start", taskId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.ruleCode").value("VERSION_MISMATCH"));

        assertEvent(pilot.projectId(), "TASK_CREATED");
        assertAudit("/api/v1/admin/tasks/" + taskId + "/start", "FAIL");
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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
                .andExpect(jsonPath("$.data.level").value("MEDIUM"))
                .andExpect(jsonPath("$.data.score").value(35))
                .andExpect(jsonPath("$.data.reasons[0]").value("距离交期仅 1 天"))
                .andExpect(jsonPath("$.data.suggestion").isString());
        assertEvent(pilot.projectId(), "EXECUTION_EXCEPTION_REPORTED");
    }

    @Test
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

    @Test
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

        mockMvc.perform(get("/api/v1/mp/dashboard")
                        .param("period", "CUSTOM")
                        .param("startDate", LocalDate.now().minusDays(3).toString())
                        .param("endDate", LocalDate.now().toString())
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.period").value("CUSTOM"))
                .andExpect(jsonPath("$.data.deliveryTrend.length()").value(4));

        mockMvc.perform(get("/api/v1/admin/dashboard")
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

    @Test
    void dashboardDoesNotExposeNonMemberProjectMetrics() throws Exception {
        BasicProject project = createBasicProject();
        String username = "isolated-tech-" + Long.toUnsignedString(System.nanoTime());
        mockMvc.perform(post("/api/v1/admin/system/users")
                        .header("Authorization", bearer(project.adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"isolated-tech-password\",\"nickname\":\"Isolated Tech\",\"roleCodes\":[\"technical\"]}"))
                .andExpect(status().isOk());
        String technicalToken = loginToken(username, "isolated-tech-password");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").value(0))
                .andExpect(jsonPath("$.data.projects.length()").value(0));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(project.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.activeProjects").isNumber())
                .andExpect(jsonPath("$.data.projects").isArray());
    }

    @Test
    void ac07DeniesProjectReadForNonMember() throws Exception {
        BasicProject pilot = createBasicProject();
        String technicalToken = loginToken("demo-tech", "demo-password-123");

        mockMvc.perform(get("/api/v1/admin/projects/{id}", pilot.projectId())
                        .header("Authorization", bearer(technicalToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data.ruleCode").value("PROJECT_DATA_SCOPE"));

        assertAudit("/api/v1/admin/projects/" + pilot.projectId(), "FAIL");
    }

    @Test
    void ac08RejectsIdempotentReplayAndStaleTaskVersion() throws Exception {
        PilotProject pilot = createTechnicalPilot();
        String requestId = "pilot-task-" + System.nanoTime();
        String taskPayload = "{\"projectId\":" + pilot.projectId() + ",\"taskType\":\"PURCHASE\",\"title\":\"幂等采购任务\",\"referencedVersion\":\"" + pilot.versionNo() + "\",\"responsibleName\":\"采购员\",\"assigneeId\":" + userId("api-admin") + "}";
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
        String technicalUsername = "demo-tech";
        addProjectMember(project, technicalUsername, "TECHNICAL");
        addProjectMember(project, "demo-production", "PRODUCTION");
        addProjectMember(project, "demo-quality", "QUALITY");
        String technicalToken = loginToken(technicalUsername, "demo-password-123");
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
                        .header("Authorization", bearer(project.adminToken())))
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
                .andExpect(jsonPath("$.data.memberName").value(username));
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

    private void submitSampleForConfirmation(PilotProject pilot, long sampleId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/samples/{id}/submit-confirm", sampleId)
                        .header("Authorization", bearer(pilot.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAIT_CUSTOMER_CONFIRM"));
    }

    private long createTask(PilotProject pilot, String taskType, String title, String referencedVersion) throws Exception {
        return createTask(pilot, taskType, title, referencedVersion, "api-admin");
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
