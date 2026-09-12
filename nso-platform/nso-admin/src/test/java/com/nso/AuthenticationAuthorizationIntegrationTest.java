package com.nso;

import org.junit.jupiter.api.Test;

class AuthenticationAuthorizationIntegrationTest extends NsoApplicationTests {

    @Test
    void protectedApiReturnsStructuredUnauthorizedResponse() throws Exception {
        super.protectedApiReturnsStructuredUnauthorizedResponse();
    }

    @Test
    void publicAccountSupportEndpointsHideAccountExistenceAndAdminCanReadTicketQueue() throws Exception {
        super.publicAccountSupportEndpointsHideAccountExistenceAndAdminCanReadTicketQueue();
    }

    @Test
    void securityControlsRejectInvalidAndRevokedTokensRefreshReplayAndFailedLoginBursts() throws Exception {
        super.securityControlsRejectInvalidAndRevokedTokensRefreshReplayAndFailedLoginBursts();
    }

    @Test
    void rateLimiterRejectsRequestsAfterTheConfiguredWindowQuota() {
        super.rateLimiterRejectsRequestsAfterTheConfiguredWindowQuota();
    }

    @Test
    void rateLimiterRepairsLegacyKeyWithoutExpiration() throws Exception {
        super.rateLimiterRepairsLegacyKeyWithoutExpiration();
    }

    @Test
    void loginFailureCounterRepairsLegacyKeyWithoutExpiration() throws Exception {
        super.loginFailureCounterRepairsLegacyKeyWithoutExpiration();
    }

    @Test
    void administratorCanMaintainOrganizationRolesMenusAndUsers() throws Exception {
        super.administratorCanMaintainOrganizationRolesMenusAndUsers();
    }

    @Test
    void demoRolesExposeOnlyTheirGrantedFunctionsAndPermissionChangesRevokeOldTokens() throws Exception {
        super.demoRolesExposeOnlyTheirGrantedFunctionsAndPermissionChangesRevokeOldTokens();
    }

    @Test
    void fieldRoleAndAdministrativeToolsHonorTheV12PermissionBoundary() throws Exception {
        super.fieldRoleAndAdministrativeToolsHonorTheV12PermissionBoundary();
    }

    @Test
    void profileUpdatesAreScopedOptimisticAndPasswordChangeRevokesTheCurrentSession() throws Exception {
        super.profileUpdatesAreScopedOptimisticAndPasswordChangeRevokesTheCurrentSession();
    }

    @Test
    void demoScenarioInitializerCanRunAgainWithoutDuplicateRoleFixtures() throws Exception {
        super.demoScenarioInitializerCanRunAgainWithoutDuplicateRoleFixtures();
    }
}
