package com.nso;

import org.junit.jupiter.api.Test;

class PublicConfirmationIntegrationTest extends NsoApplicationTests {

    @Test
    void customerAndPublicConfirmationEndpointsRedactInternalSampleFields() throws Exception {
        super.customerAndPublicConfirmationEndpointsRedactInternalSampleFields();
    }

    @Test
    void sampleWorkflowCreatesAssignedTasksAndSupportsConditionalConfirmation() throws Exception {
        super.sampleWorkflowCreatesAssignedTasksAndSupportsConditionalConfirmation();
    }

    @Test
    void sampleWorkflowAssignsTasksFromModernProjectResponsibilities() throws Exception {
        super.sampleWorkflowAssignsTasksFromModernProjectResponsibilities();
    }
}
