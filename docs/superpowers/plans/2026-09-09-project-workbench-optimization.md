# 项目工作台一期优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目、行动、变更和交付四条路径统一为项目经理可在 30 秒内识别阶段、阻塞、责任人、期限和唯一下一步的推进工作台。

**Architecture:** 复用现有 `ProjectWorkspaceDto`、`ProjectPulseDto`、`ActionItemDto` 与 `nso_action_item` 投影表，补充统一状态/风险/截止时间映射和工作台聚合字段。前端保留左侧导航和横向业务信息，以 `projectId` 深链接维持项目上下文；所有业务状态由服务层转换为中文文案，控制器只负责权限、参数和响应封装。

**Tech Stack:** Java 17、Spring Boot 3.5、MyBatis-Plus、Flyway、JUnit/Testcontainers、Vue 3、TypeScript、Element Plus、Vite。

**Spec:** 用户提供的《非标订单协同平台一期优化计划》及本会话确认的实施设计。

## Global Constraints

- 一期范围限定为项目工作台、我的行动、变更和交付；保留现有阶段流程与左侧导航。
- 生产数据库变更只能新增 `nso-admin/src/main/resources/db/migration` Flyway 脚本，不修改已发布迁移。
- `nso-system` 只能通过已有 Port 接口使用缓存、锁和存储；Controller 不直接调用 Mapper。
- 对外响应禁止渲染内部枚举编号或编码，统一提供业务文案、颜色/风险等级和截止状态。
- 所有关键写操作维持租户隔离、权限校验、事务、幂等和并发控制。

### Task 1: 统一状态、行动与工作台契约

**Files:**
- Create: `nso-platform/nso-system/src/main/java/com/nso/business/core/ProjectProgressSemantics.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/core/NsoDtos.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/report/service/IReportService.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/report/service/impl/ReportServiceImpl.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/support/action/IActionCenterService.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/support/action/ActionCenterServiceImpl.java`
- Modify: `nso-platform/nso-admin/src/main/java/com/nso/web/controller/business/ActionCenterController.java`
- Modify: `nso-platform/nso-admin/src/main/java/com/nso/web/controller/business/ReportController.java`
- Test: `nso-platform/nso-admin/src/test/java/com/nso/ProjectWorkbenchContractTest.java`

**Interfaces:**
- Produces `ProjectProgressSemantics.stageLabel/statusLabel/riskLabel/dueState` and a deterministic `recommendation` object.
- Produces workbench fields for `currentStage`, `completionCriteria`, `nextAction`, `blockers`, `pendingDecisions`, `versionConflicts`, `changeSummary`, `deliveryReadiness`.
- Produces action query filters `projectId`, `assigneeUserId`, `sourceType`, `priority`, `dueState` while preserving existing callers.

- [ ] Add failing unit/integration assertions for Chinese status mapping, overdue priority, project workbench aggregation, and action filters.
- [ ] Run the focused tests and capture the failure before implementation.
- [ ] Implement the semantics mapper and DTO fields with backward-compatible JSON names where existing frontend consumers depend on them.
- [ ] Update report/action services to aggregate visible project data, rank blockers by priority then due time, and choose exactly one recommended action.
- [ ] Expose the new filters and workbench fields through controllers with existing authorization and tenant scope.
- [ ] Run focused tests, then the affected Maven module tests.

### Task 2: Project list and workspace推进主线

**Files:**
- Modify: `nso-platform/nso-web/src/types/index.ts`
- Modify: `nso-platform/nso-web/src/api/index.ts`
- Modify: `nso-platform/nso-web/src/views/project/index.vue`
- Modify: `nso-platform/nso-web/src/styles/pages/project.css`
- Modify: `nso-platform/nso-web/src/router/index.ts`
- Test: `nso-platform/nso-web/scripts/layout-contract.test.mjs` or a focused component contract test if available.

**Interfaces:**
- Consumes the Task 1 workbench response and action routes.
- Produces project cards sorted by risk/due/blocker/decision priority and a fixed top progress strip with one primary action.

- [ ] Add frontend type/contract assertions for the new workbench fields and route query preservation.
- [ ] Update project API/types and project list sorting/display with stage, risk, open blockers, pending decisions, due state and recommended action.
- [ ] Add the workspace top mainline: stage, completion condition, next action, owner, due time and blocked state.
- [ ] Keep technical, sample, change, event and delivery sections as contextual deep links; preserve selected `projectId` on navigation and return.
- [ ] Run `npm run type-check` and the focused layout contract.

### Task 3: My Actions personal execution queue

**Files:**
- Modify: `nso-platform/nso-web/src/types/index.ts`
- Modify: `nso-platform/nso-web/src/api/index.ts`
- Modify: `nso-platform/nso-web/src/views/action/index.vue`
- Modify: `nso-platform/nso-web/src/styles/pages/action.css` (create if the page currently relies only on shared styles)
- Test: `nso-platform/nso-admin/src/test/java/com/nso/DeliveryActionCenterIntegrationTest.java` or a new focused action integration test.

**Interfaces:**
- Consumes `ActionItemDto` with `projectId`, `sourceType`, `assignee`, `slaDueAt`, `priority`, `summary` and `route`.
- Produces a default personal queue plus explicit filters and one-click route navigation.

- [ ] Add failing assertions that current-user visibility excludes unrelated open actions and that resolved sources leave the queue.
- [ ] Implement project/source/risk/due filters and normalize message candidates only when they have an actionable target.
- [ ] Render “处理什么、为什么现在处理、跳到哪里处理”, owner, due state and readable source/status labels.
- [ ] Preserve project context in route query and refresh action/workbench state after returning from a detail action.
- [ ] Run action integration tests and frontend type/build checks.

### Task 4: Change impact and feedback closure

**Files:**
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/core/NsoDtos.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/change/service/impl/ChangeServiceImpl.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/support/approval/ApprovalServiceImpl.java`
- Modify: `nso-platform/nso-admin/src/main/java/com/nso/web/controller/business/ChangeController.java`
- Modify: `nso-platform/nso-web/src/types/index.ts`
- Modify: `nso-platform/nso-web/src/api/index.ts`
- Modify: `nso-platform/nso-web/src/views/change/index.vue`
- Modify: `nso-platform/nso-web/src/styles/pages/change.css`
- Test: `nso-platform/nso-admin/src/test/java/com/nso/ProjectChangeStateMachineIntegrationTest.java`

**Interfaces:**
- Produces change cards with affected stage, bound technical version, departments, outstanding feedback, approval gap, due impact, owner and next action.
- Keeps close/revoke/feedback transitions guarded until required responses and approvals are complete.

- [ ] Add failing assertions for incomplete feedback/approval visibility and project-context links.
- [ ] Extend change impact DTO/service projection and approval synchronization with responsibility, version and response status.
- [ ] Update change API/controller and page labels, filters, cards and deep links.
- [ ] Add workbench refresh behavior after feedback, approval or close transitions.
- [ ] Run change state-machine tests and frontend build.

### Task 5: Delivery precheck closed loop and readable history

**Files:**
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/core/NsoDtos.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/business/task/service/impl/TaskServiceImpl.java`
- Modify: `nso-platform/nso-system/src/main/java/com/nso/web/controller/business/TaskController.java`
- Modify: `nso-platform/nso-web/src/types/index.ts`
- Modify: `nso-platform/nso-web/src/api/index.ts`
- Modify: `nso-platform/nso-web/src/views/task/deliveries.vue`
- Modify: `nso-platform/nso-web/src/styles/pages/task.css`
- Test: `nso-platform/nso-admin/src/test/java/com/nso/DeliveryActionCenterIntegrationTest.java`
- Test: `nso-platform/nso-admin/src/test/java/com/nso/NsoApplicationTests.java`

**Interfaces:**
- Produces precheck rows containing status, owner, evidence, blocker reason and readable business labels.
- Enforces `precheck -> blocker resolution -> submit -> receipt/feedback` and maps historical delivery enums to logistics, receipt and customer-feedback copy.

- [ ] Add failing assertions that any precheck blocker prevents submission and that history contains readable labels rather than enum codes.
- [ ] Extend delivery readiness/history DTOs and service mapping while keeping existing delivery state transitions.
- [ ] Update delivery page to show the four-step loop, responsible owner, evidence and blocker resolution entry points.
- [ ] Link delivery actions back to the selected project workspace and refresh the workbench after submission/feedback.
- [ ] Run delivery integration tests and frontend build.

### Task 6: End-to-end acceptance and regression verification

**Files:**
- Modify: `nso-platform/nso-admin/src/test/java/com/nso/NsoApplicationTests.java`
- Modify: `nso-platform/nso-web/scripts/layout-contract.test.mjs`
- Create: `nso-platform/docs/testing/project-workbench-acceptance.md`

- [ ] Add an integration path for project manager, technical owner and production/quality collaborators covering stage progression, version conflict, overdue action, change feedback and delivery block.
- [ ] Add frontend contract checks for 30-second information hierarchy, one-action route links and project query retention.
- [ ] Run `mvn clean test` with Docker/Testcontainers available.
- [ ] Run `npm run build` and `docker compose config`.
- [ ] Start the dev backend/frontend without concurrent clean/build processes and verify `/actuator/health`, `/` and `/system/accounts`.
- [ ] Record evidence and residual limits in the acceptance document.

