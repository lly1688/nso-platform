package com.nso;

import org.junit.jupiter.api.Test;

class MigrationIntegrationTest extends NsoApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate migrationJdbc;

    @Test
    void keepsOneCompleteUserLifecycleIndexAfterUpgrade() {
        org.junit.jupiter.api.Assertions.assertEquals(0, migrationJdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sys_user' AND index_name = 'idx_sys_user_directory'", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals("tenant_id,user_type,status,dept_id", migrationJdbc.queryForObject(
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_user' "
                        + "AND index_name = 'idx_sys_user_lifecycle'", String.class));
    }

    @Test
    void applicationContextLoads() {
        super.contextLoads();
    }

    @Test
    void migratesV1DataToTenantGovernanceAndKeepsRequestIdempotency() {
        super.migratesV1DataToTenantGovernanceAndKeepsRequestIdempotency();
    }
}
