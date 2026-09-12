package com.nso;

import com.nso.business.core.ProjectProgressSemantics;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectProgressSemanticsTest {

    @Test
    void customerConfirmingProjectShowsActionableCompletionCondition() {
        var progress = ProjectProgressSemantics.describe("CONFIRM", "CUSTOMER_CONFIRMING", LocalDate.now().plusDays(2), 0, 0);

        assertEquals("客户确认", progress.stageLabel());
        assertEquals("完成样品确认并记录客户结论", progress.completionCriteria());
        assertEquals("优先催办客户确认", progress.recommendedAction().label());
        assertEquals("DUE_SOON", progress.dueState());
        assertTrue(progress.riskLevel() != null);
    }

    @Test
    void overdueBlockerWinsOverStageAction() {
        var progress = ProjectProgressSemantics.describe("TECHNICAL", "TECH_PREPARING", LocalDate.now().minusDays(1), 1, 0);

        assertEquals("OVERDUE", progress.dueState());
        assertEquals("处理逾期阻塞", progress.recommendedAction().label());
    }

    @Test
    void terminalProjectDoesNotBecomeOverdueFromHistoricalTargetDate() {
        var progress = ProjectProgressSemantics.describe("DELIVERY", "ARCHIVED", LocalDate.now().minusDays(30), 0, 0);

        assertEquals("ON_TRACK", progress.dueState());
        assertEquals("查看项目记录", progress.recommendedAction().label());
    }
}
