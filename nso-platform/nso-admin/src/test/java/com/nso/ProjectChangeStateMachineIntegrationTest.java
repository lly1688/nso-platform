package com.nso;

import org.junit.jupiter.api.Test;

class ProjectChangeStateMachineIntegrationTest extends NsoApplicationTests {

    @Test
    void authenticatedUserCanCreateCustomerProjectRequirementAndReview() throws Exception {
        super.authenticatedUserCanCreateCustomerProjectRequirementAndReview();
    }

    @Test
    void projectListReturnsRealChangeCountsPerProject() throws Exception {
        super.projectListReturnsRealChangeCountsPerProject();
    }

    @Test
    void listEndpointsUseNormalizedPageResultContract() throws Exception {
        super.listEndpointsUseNormalizedPageResultContract();
    }

    @Test
    void projectMembersUseDirectoryIdentityAndSupportResponsibilityLifecycle() throws Exception {
        super.projectMembersUseDirectoryIdentityAndSupportResponsibilityLifecycle();
    }

    @Test
    void replacingProjectTechnicalOwnerReleasesTheExistingUniqueOwnerSlot() throws Exception {
        super.replacingProjectTechnicalOwnerReleasesTheExistingUniqueOwnerSlot();
    }

    @Test
    void v10MainLoopRunsFromReviewedRequirementToDeliveryAndArchive() throws Exception {
        super.v10MainLoopRunsFromReviewedRequirementToDeliveryAndArchive();
    }

    @Test
    void v10BlocksManualProjectJumpsAndUnpreparedSampleSubmission() throws Exception {
        super.v10BlocksManualProjectJumpsAndUnpreparedSampleSubmission();
    }

    @Test
    void v10DeliveryReadinessReportsBlocksAndPendingMilestonesHaveNoDate() throws Exception {
        super.v10DeliveryReadinessReportsBlocksAndPendingMilestonesHaveNoDate();
    }

    @Test
    void qualityAssigneeCanStartAssignedInspectionTask() throws Exception {
        super.qualityAssigneeCanStartAssignedInspectionTask();
    }

    @Test
    void taskPlanningRequiresMatchingAssigneeResponsibilityAndBindsCurrentPublishedVersion() throws Exception {
        super.taskPlanningRequiresMatchingAssigneeResponsibilityAndBindsCurrentPublishedVersion();
    }

    @Test
    void ac01BlocksStaleDocumentVersionAndRecordsAudit() throws Exception {
        super.ac01BlocksStaleDocumentVersionAndRecordsAudit();
    }

    @Test
    void ac02BlocksProductionUntilSampleConfirmed() throws Exception {
        super.ac02BlocksProductionUntilSampleConfirmed();
    }

    @Test
    void ac03CreatesSixChangeImpactsApprovesAndBlocksAffectedTasks() throws Exception {
        super.ac03CreatesSixChangeImpactsApprovesAndBlocksAffectedTasks();
    }

    @Test
    void ac04ClosesChangeAfterAllImpactsReceiveFeedback() throws Exception {
        super.ac04ClosesChangeAfterAllImpactsReceiveFeedback();
    }

    @Test
    void ac08RejectsIdempotentReplayAndStaleTaskVersion() throws Exception {
        super.ac08RejectsIdempotentReplayAndStaleTaskVersion();
    }
}
