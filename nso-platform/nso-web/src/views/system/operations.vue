<script setup lang="ts">
// 运维审计与风险扫描状态。
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { AuditLog } from '@/types';
import { emptyPage, optional, pageList } from './shared';

const loading = ref(false);
const jobs = ref<Array<Record<string, unknown>>>([]);
const audits = ref<AuditLog[]>([]);
const authorizationRequests = ref<Array<Record<string, unknown>>>([]);
const retentionPolicies = ref<Array<Record<string, unknown>>>([]);
const jobPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'job' });
const auditPagination = usePagination<AuditLog>({ queryPrefix: 'audit' });
const authorizationPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'authorization' });
const retentionPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'retention' });
jobPagination.configure((params) => api.jobs(params), (result) => { jobs.value = result.list; });
auditPagination.configure((params) => api.auditLogs(params), (result) => { audits.value = result.list; });
authorizationPagination.configure((params) => api.authorizationRequests(params), (result) => { authorizationRequests.value = result.list; });
retentionPagination.configure((params) => api.retentionPolicies(params), (result) => { retentionPolicies.value = result.list; });

async function load() {
    loading.value = true;
    try {
        await Promise.all([
            jobPagination.reload(),
            auditPagination.reload(),
            authorizationPagination.reload(),
            retentionPagination.reload()
        ]);
    }
    finally {
        loading.value = false;
    }
}

async function runJob(row: Record<string, unknown>) {
    await api.runJob(Number(row.id));
    ElMessage.success('定时任务已手动执行');
    await load();
}

async function approveProtectedRole(row: Record<string, unknown>) {
    try {
        await api.approveProtectedRole(Number(row.id));
        ElMessage.success('已完成第二人复核并生效');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '复核失败');
    }
}

async function requestProtectedRole() {
    try {
        const userId = (await ElMessageBox.prompt('输入内部账号 ID', '受保护角色申请')).value;
        const roleCode = (await ElMessageBox.prompt('输入 superadmin、system_admin 或 hr_admin', '受保护角色申请', {
            inputPattern: /^(superadmin|system_admin|hr_admin)$/,
            inputErrorMessage: '角色编码不合法'
        })).value;
        const reason = (await ElMessageBox.prompt('填写最小权限授予理由', '受保护角色申请')).value;
        await api.requestProtectedRole({ userId: Number(userId), roleCode, reason });
        ElMessage.success('申请已提交，须由另一名超级管理员复核');
        await load();
    }
    catch {
        // 未完成第二人复核时不提交角色申请。
    }
}

async function runRetention(row: Record<string, unknown>) {
    try {
        const code = String(row.policyCode);
        const confirmationRef = code === 'BUSINESS_HISTORY' || code === 'AUTHORIZATION_AUDIT'
            ? (await ElMessageBox.prompt('请输入已完成备份导出与二次确认编号；业务历史默认不物理删除', '受控清理确认')).value
            : undefined;
        await api.runRetentionPolicy(code, confirmationRef);
        ElMessage.success('保留策略已执行并留下审计记录');
        await load();
    }
    catch {
        // 受控清理确认被关闭后保持原策略。
    }
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 运维操作与审计记录 -->
    <div class="system-page" v-loading="loading">
        <section class="two-column">
            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">定时任务白名单</span>
                </div>
                <!-- 数据表格 -->
                <el-table :data="jobs" height="320" size="small">
                    <el-table-column prop="jobName" label="任务" width="160" />
                    <el-table-column prop="beanName" label="Bean" min-width="180" />
                    <el-table-column prop="cronExpression" label="Cron" width="160" />
                    <el-table-column prop="status" label="状态" width="110" />
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="runJob(row)">执行</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="jobPagination.pageNo"
                    :page-size="jobPagination.pageSize"
                    :total="jobPagination.total"
                    :loading="jobPagination.loading"
                    @update:page-no="jobPagination.goTo"
                    @update:page-size="jobPagination.changePageSize"
                />
            </div>

            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">操作审计</span>
                </div>
                <el-table :data="audits" height="320" size="small">
                    <el-table-column prop="moduleName" label="模块" width="120" />
                    <el-table-column prop="operationType" label="操作" width="120" />
                    <el-table-column prop="businessType" label="对象" width="120" />
                    <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="result" label="结果" width="90" />
                </el-table>
                <PaginationBar
                    :page-no="auditPagination.pageNo"
                    :page-size="auditPagination.pageSize"
                    :total="auditPagination.total"
                    :loading="auditPagination.loading"
                    @update:page-no="auditPagination.goTo"
                    @update:page-size="auditPagination.changePageSize"
                />
            </div>
        </section>

        <section class="two-column">
            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">授权申请与双人复核</span>
                </div>
                <div class="panel-body stack">
                    <p class="system-note">superadmin、system_admin、hr_admin 不可通过账号编辑直接授予；申请人和复核人必须是不同的超级管理员。</p>
                    <el-button type="primary" @click="requestProtectedRole">发起受保护角色申请</el-button>
                </div>
                <el-table :data="authorizationRequests" height="260" size="small">
                    <el-table-column prop="targetUserId" label="目标账号 ID" width="120" />
                    <el-table-column prop="requestedRoleCode" label="申请角色" width="150" />
                    <el-table-column prop="reason" label="理由" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="status" label="状态" width="110" />
                    <el-table-column label="操作" width="100" fixed="right">
                        <template #default="{ row }">
                            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="approveProtectedRole(row)">复核</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="authorizationPagination.pageNo"
                    :page-size="authorizationPagination.pageSize"
                    :total="authorizationPagination.total"
                    :loading="authorizationPagination.loading"
                    @update:page-no="authorizationPagination.goTo"
                    @update:page-size="authorizationPagination.changePageSize"
                />
            </div>

            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">归档与保留策略</span>
                </div>
                <div class="panel-body">
                    <p class="system-note">业务、审计和交接历史默认只读归档，不自动删除；仅令牌、验证码及临时导入导出数据可自动清理。</p>
                </div>
                <el-table :data="retentionPolicies" height="300" size="small">
                    <el-table-column prop="policyCode" label="策略" min-width="170" />
                    <el-table-column prop="retentionDays" label="保留天数" width="100" />
                    <el-table-column prop="archiveAfterDays" label="归档天数" width="100" />
                    <el-table-column prop="enabled" label="启用" width="80">
                        <template #default="{ row }">
                            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="100" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="runRetention(row)">执行</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="retentionPagination.pageNo"
                    :page-size="retentionPagination.pageSize"
                    :total="retentionPagination.total"
                    :loading="retentionPagination.loading"
                    @update:page-no="retentionPagination.goTo"
                    @update:page-size="retentionPagination.changePageSize"
                />
            </div>
        </section>
    </div>
</template>
