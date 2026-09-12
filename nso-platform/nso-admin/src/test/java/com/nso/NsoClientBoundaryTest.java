package com.nso;

import com.nso.framework.security.NsoClientBoundary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NsoClientBoundaryTest {
    @Test
    void onlyAdminClientMayUseAdminRoute() {
        assertThat(NsoClientBoundary.allows("/api/v1/admin/dashboard", "admin")).isTrue();
        assertThat(NsoClientBoundary.allows("/api/v1/admin/dashboard", "mp")).isFalse();
        assertThat(NsoClientBoundary.allows("/api/v1/admin/dashboard", null)).isFalse();
    }

    @Test
    void contextPathIsIgnoredWhenCheckingRoute() {
        assertThat(NsoClientBoundary.allows("/nso/api/v1/admin/dashboard", "admin")).isTrue();
        assertThat(NsoClientBoundary.allows("/nso/api/v1/admin/dashboard", "mp")).isFalse();
    }
}
