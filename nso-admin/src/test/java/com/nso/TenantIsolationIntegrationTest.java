package com.nso;

import org.junit.jupiter.api.Test;

class TenantIsolationIntegrationTest extends NsoApplicationTests {

    @Test
    void tenantInterceptorPreventsCrossTenantCustomerRead() {
        super.tenantInterceptorPreventsCrossTenantCustomerRead();
    }

    @Test
    void dashboardAggregatesScopedDataAndValidatesPeriods() throws Exception {
        super.dashboardAggregatesScopedDataAndValidatesPeriods();
    }

    @Test
    void dashboardDoesNotExposeNonMemberProjectMetrics() throws Exception {
        super.dashboardDoesNotExposeNonMemberProjectMetrics();
    }

    @Test
    void ac07DeniesProjectReadForNonMember() throws Exception {
        super.ac07DeniesProjectReadForNonMember();
    }
}
