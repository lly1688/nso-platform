<script setup lang="ts">
// 密码恢复与支持工单处理状态。
import { onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { PasswordRecoveryRequest, SupportTicket, SupportTicketAttachment } from '@/types';
import { pageList } from './shared';
import { userFacingError } from '@/utils/request';

const loading = ref(false);
const recoveryStatus = ref<PasswordRecoveryRequest['status'] | ''>('');
const ticketStatus = ref<SupportTicket['status'] | ''>('');
const recoveries = ref<PasswordRecoveryRequest[]>([]);
const tickets = ref<SupportTicket[]>([]);
const recoveryPagination = usePagination<PasswordRecoveryRequest>({ queryPrefix: 'recovery' });
const ticketPagination = usePagination<SupportTicket>({ queryPrefix: 'ticket' });
recoveryPagination.configure(
    (params) => api.passwordRecoveryRequests({ status: recoveryStatus.value || undefined, ...params }),
    (result) => {
        recoveries.value = result.list;
    }
);
ticketPagination.configure(
    (params) => api.supportTickets({ status: ticketStatus.value || undefined, ...params }),
    (result) => {
        tickets.value = result.list;
    }
);

const recoveryStatusOptions = [
    { label: '全部状态', value: '' },
    { label: '待处理', value: 'PENDING' },
    { label: '核验中', value: 'IN_REVIEW' },
    { label: '已重置', value: 'RESET' },
    { label: '已拒绝', value: 'REJECTED' }
];
const ticketStatusOptions = [
    { label: '全部状态', value: '' },
    { label: '待处理', value: 'OPEN' },
    { label: '处理中', value: 'PROCESSING' },
    { label: '已解决', value: 'RESOLVED' },
    { label: '已关闭', value: 'CLOSED' }
];

async function load() {
    loading.value = true;
    try {
        await Promise.all([recoveryPagination.reload(), ticketPagination.reload()]);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '账户支持队列加载失败'));
    }
    finally {
        loading.value = false;
    }
}

watch(recoveryStatus, () => {
    void recoveryPagination.reset().catch((error) => ElMessage.error(userFacingError(error, '密码恢复申请加载失败')));
});
watch(ticketStatus, () => {
    void ticketPagination.reset().catch((error) => ElMessage.error(userFacingError(error, '支持工单加载失败')));
});

async function promptNote(title: string, message: string) {
    try {
        const { value } = await ElMessageBox.prompt(message, title, {
            inputType: 'textarea',
            inputPlaceholder: '请填写处理说明',
            inputValidator: (input) => input?.trim() ? true : '处理说明不能为空',
            confirmButtonText: '确认',
            cancelButtonText: '取消'
        });
        return value.trim();
    }
    catch {
        return undefined;
    }
}

async function reviewRecovery(row: PasswordRecoveryRequest) {
    const note = await promptNote('开始身份核验', `为账号 ${row.username} 记录核验说明`);
    if (!note) return;
    try {
        await api.reviewPasswordRecovery(row.id, { status: 'IN_REVIEW', handlingNote: note, version: row.version });
        ElMessage.success('恢复申请已进入人工核验');
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '状态更新失败'));
    }
}

async function rejectRecovery(row: PasswordRecoveryRequest) {
    const note = await promptNote('拒绝恢复申请', `说明拒绝账号 ${row.username} 的原因`);
    if (!note) return;
    try {
        await api.reviewPasswordRecovery(row.id, { status: 'REJECTED', handlingNote: note, version: row.version });
        ElMessage.success('恢复申请已拒绝');
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '恢复申请处理失败'));
    }
}

async function resetPassword(row: PasswordRecoveryRequest) {
    const note = await promptNote('确认身份后重置密码', '填写已完成线下身份核验的说明；生成的临时密码仅显示一次。');
    if (!note) return;
    try {
        const result = await api.resetRecoveredPassword(row.id, { handlingNote: note, version: row.version });
        await ElMessageBox.alert(`临时密码：${result.temporaryPassword}\n\n请通过已核验的线下渠道告知用户。该密码不会再次显示，用户登录后必须立即修改。`, '临时密码已生成', {
            confirmButtonText: '我已记录',
            closeOnClickModal: false,
            closeOnPressEscape: false
        });
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '密码重置失败'));
    }
}

async function advanceTicket(row: SupportTicket, status: 'PROCESSING' | 'RESOLVED' | 'CLOSED', label: string) {
    const note = await promptNote(label, `为工单 ${row.ticketNo} 填写处理说明`);
    if (!note) return;
    try {
        await api.updateSupportTicket(row.id, { status, handlingNote: note, version: row.version });
        ElMessage.success(`工单已${label}`);
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '工单处理失败'));
    }
}

async function openAttachment(ticket: SupportTicket, attachment: SupportTicketAttachment) {
    try {
        const blob = await api.downloadSupportTicketImage(ticket.id, attachment.id);
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '图片打开失败'));
    }
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 账户支持查询与处理列表 -->
    <div class="system-page account-support-page" v-loading="loading">
        <section class="panel">
            <div class="panel-header account-support-page__header">
                <div>
                    <span class="panel-title">密码恢复申请</span>
                    <p>仅在完成线下身份核验后生成临时密码；系统会立即撤销旧会话。</p>
                </div>
                <el-select v-model="recoveryStatus" aria-label="恢复申请状态">
                    <el-option v-for="item in recoveryStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </div>
            <!-- 数据表格 -->
            <el-table :data="recoveries" size="small" max-height="350">
                <el-table-column prop="username" label="账号" width="130" />
                <el-table-column prop="contactName" label="申请人" width="110" />
                <el-table-column prop="contactValue" label="联系方式" min-width="150" show-overflow-tooltip />
                <el-table-column prop="requesterNote" label="补充说明" min-width="160" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.status === 'RESET' ? 'success' : row.status === 'REJECTED' ? 'info' : row.status === 'IN_REVIEW' ? 'warning' : 'danger'">{{ row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="handlingNote" label="处理说明" min-width="170" show-overflow-tooltip />
                <el-table-column label="操作" width="210" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="row.status === 'PENDING'" link type="primary" @click="reviewRecovery(row)">开始核验</el-button>
                        <el-button v-if="row.status === 'PENDING' || row.status === 'IN_REVIEW'" link type="danger" @click="rejectRecovery(row)">拒绝</el-button>
                        <el-button v-if="row.status === 'IN_REVIEW'" link type="warning" @click="resetPassword(row)">生成临时密码</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="recoveryPagination.pageNo"
                :page-size="recoveryPagination.pageSize"
                :total="recoveryPagination.total"
                :loading="recoveryPagination.loading"
                @update:page-no="recoveryPagination.goTo"
                @update:page-size="recoveryPagination.changePageSize"
            />
        </section>

        <section class="panel">
            <div class="panel-header account-support-page__header">
                <div>
                    <span class="panel-title">技术支持工单</span>
                    <p>登录前请求由管理员线下联系；登录用户可在帮助抽屉查看处理进度。</p>
                </div>
                <el-select v-model="ticketStatus" aria-label="支持工单状态">
                    <el-option v-for="item in ticketStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </div>
            <el-table :data="tickets" size="small" max-height="420">
                <el-table-column prop="ticketNo" label="工单号" width="145" />
                <el-table-column label="请求人" width="130">
                    <template #default="{ row }">
                        <div>{{ row.contactName }}</div>
                        <small>{{ row.requesterUsername || row.contactValue || '匿名' }}</small>
                    </template>
                </el-table-column>
                <el-table-column prop="category" label="分类" width="110" />
                <el-table-column prop="priority" label="紧急度" width="90">
                    <template #default="{ row }"><el-tag size="small" :type="row.priority === 'URGENT' ? 'danger' : row.priority === 'HIGH' ? 'warning' : 'info'">{{ row.priority }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="description" label="问题描述" min-width="220" show-overflow-tooltip />
                <el-table-column label="图片" width="100">
                    <template #default="{ row }">
                        <el-button v-if="row.attachments.length" link type="primary" @click="openAttachment(row, row.attachments[0])">{{ row.attachments.length }} 张</el-button>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="110">
                    <template #default="{ row }"><el-tag size="small" :type="row.status === 'RESOLVED' || row.status === 'CLOSED' ? 'success' : row.status === 'PROCESSING' ? 'warning' : 'danger'">{{ row.status }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="handlingNote" label="处理说明" min-width="170" show-overflow-tooltip />
                <el-table-column label="操作" width="155" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="row.status === 'OPEN'" link type="primary" @click="advanceTicket(row, 'PROCESSING', '受理')">受理</el-button>
                        <el-button v-if="row.status === 'PROCESSING'" link type="success" @click="advanceTicket(row, 'RESOLVED', '解决')">解决</el-button>
                        <el-button v-if="row.status === 'RESOLVED'" link type="info" @click="advanceTicket(row, 'CLOSED', '关闭')">关闭</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="ticketPagination.pageNo"
                :page-size="ticketPagination.pageSize"
                :total="ticketPagination.total"
                :loading="ticketPagination.loading"
                @update:page-no="ticketPagination.goTo"
                @update:page-size="ticketPagination.changePageSize"
            />
        </section>
    </div>
</template>

<style scoped>
/* 账户支持局部样式。 */
.account-support-page {
    display: grid;
    gap: 18px;
}

.account-support-page__header {
    align-items: flex-start;
}

.account-support-page__header > div {
    display: grid;
    gap: 5px;
}

.account-support-page__header p {
    margin: 0;
    color: var(--nso-muted);
    font-size: 12px;
    line-height: 1.5;
}

.account-support-page__header :deep(.el-select) {
    width: 130px;
}

.account-support-page small {
    color: var(--nso-faint);
    font-size: 11px;
}

@media (max-width: 680px) {
    .account-support-page__header {
        display: grid;
        gap: 12px;
    }
}
</style>
