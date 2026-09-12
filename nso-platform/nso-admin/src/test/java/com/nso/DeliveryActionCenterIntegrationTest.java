package com.nso;

import org.junit.jupiter.api.Test;

class DeliveryActionCenterIntegrationTest extends NsoApplicationTests {

    @Test
    void actionCenterProjectionDeduplicatesAndResolvesFromTaskFact() throws Exception {
        super.actionCenterProjectionDeduplicatesAndResolvesFromTaskFact();
    }

    @Test
    void capaEnforcesStateEvidenceActionsAndSerialCloseApproval() throws Exception {
        super.capaEnforcesStateEvidenceActionsAndSerialCloseApproval();
    }

    @Test
    void approvalTemplatesAreVersionedAndProjectPulseExposesDecisionSignals() throws Exception {
        super.approvalTemplatesAreVersionedAndProjectPulseExposesDecisionSignals();
    }
}
