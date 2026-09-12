package com.nso;

import org.junit.jupiter.api.Test;

class FileSchedulerIntegrationTest extends NsoApplicationTests {

    @Test
    void fileAndTaskOperationsRequireProjectScopeAssigneeAndRole() throws Exception {
        super.fileAndTaskOperationsRequireProjectScopeAssigneeAndRole();
    }

    @Test
    void projectManagerCanExecuteAssignedDeliveryTask() throws Exception {
        super.projectManagerCanExecuteAssignedDeliveryTask();
    }

    @Test
    void fileUploadRejectsMimeTypeMismatchBeforeObjectStorageAndGeneratorStaysWhitelisted() throws Exception {
        super.fileUploadRejectsMimeTypeMismatchBeforeObjectStorageAndGeneratorStaysWhitelisted();
    }

    @Test
    void quartzManualExecutionOnlyAcceptsWhitelistedJobs() throws Exception {
        super.quartzManualExecutionOnlyAcceptsWhitelistedJobs();
    }
}
