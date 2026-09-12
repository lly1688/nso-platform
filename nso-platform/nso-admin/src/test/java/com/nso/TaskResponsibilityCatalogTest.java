package com.nso;

import com.nso.business.task.service.TaskResponsibilityCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskResponsibilityCatalogTest {
    @Test
    void sampleExecutionUsesTheSameProductionOrFieldResponsibilitiesAsAssignment() {
        assertThat(TaskResponsibilityCatalog.forTaskType("SAMPLE_PREPARE"))
                .containsExactly("PRODUCTION", "FIELD_USER");
        assertThat(TaskResponsibilityCatalog.forTaskType("SAMPLE_MAKE"))
                .containsExactly("PRODUCTION", "FIELD_USER");
        assertThat(TaskResponsibilityCatalog.forTaskType("QUALITY"))
                .containsExactly("QUALITY");
    }
}
