<script setup lang="ts">
// 变更查询、影响分析与提交状态。
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { ChangeImpact, ChangeOrder, Project } from '@/types';

const projects = ref<Project[]>([]);
const changes = ref<ChangeOrder[]>([]);
const impacts = ref<ChangeImpact[]>([]);
const activeChange = ref<ChangeOrder>();
const loading = ref(false);
const changeFilter = ref('');
const auth = useAuthStore();
const form = reactive({
    projectId: undefined as number | undefined,
    changeType: 'DESIGN', urgency: 'NORMAL',
    beforeContent: '', afterContent: '', reason: ''
});
const changePagination = usePagination<ChangeOrder>();
const impactPagination = usePagination<ChangeImpact>({ queryPrefix: 'impact' });

changePagination.configure(
    (params) => api.changes({ status: changeFilter.value || undefined, ...params }),
    (result) => {
        changes.value = result.list;
    }
);

impactPagination.configure(
    (params) => activeChange.value
        ? api.impacts(activeChange.value.id, params)
        : Promise.resolve({ list: [], total: 0, pageNo: params.pageNo, pageSize: params.pageSize }),
    (result) => {
        impacts.value = result.list;
    }
);

const statusTag = (status: string) => ({ DRAFT: 'info', WAIT_IMPACT: 'warning', WAIT_APPROVAL: 'warning', EXECUTING: 'warning', CLOSED: 'success' } as Record<string, string>)[status] || 'info';
const statusLabel = (status: string) => ({ DRAFT: '草稿/已驳回', WAIT_IMPACT: '待影响分析', WAIT_APPROVAL: '待审批', EXECUTING: '执行中', CLOSED: '已关闭' } as Record<string, string>)[status] || status;
const changeTypeLabel = (type: string) => ({ DESIGN: '设计', DOCUMENT: '技术文件', MATERIAL_SPEC: '材料', PROCESS: '工艺', QUANTITY: '数量', DELIVERY: '交期', PACKAGING: '包装' } as Record<string, string>)[type] || type;
const impactTypeTag = (type: string) => ({ DOCUMENT: 'info', BOM: 'success', INSPECTION: 'info', SAMPLE: 'warning', PROCUREMENT: 'danger', PRODUCTION: 'danger' } as Record<string, string>)[type] || 'info';
const impactTypeLabel = (type: string) => ({ DOCUMENT: '图纸', BOM: 'BOM', INSPECTION: '检验规范', SAMPLE: '样品', PROCUREMENT: '采购任务', PRODUCTION: '生产任务' } as Record<string, string>)[type] || type;
async function load() {
    loading.value = true;
    try {
        projects.value = ((await api.projects()) as any).list || [];
        await changePagination.reload();
        if (activeChange.value) {
            await impactPagination.reload();
        }
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '数据加载失败'));
    }
    finally {
        loading.value = false;
    }
}

async function createChange() {
    if (!form.projectId) {
        ElMessage.warning('请选择项目');
        return;
    }
    if (!form.afterContent.trim()) {
        ElMessage.warning('请填写变更后内容');
        return;
    }
    try {
        await api.createChange({ ...form, afterContent: form.afterContent.trim() });
        ElMessage.success('变更单已发起');
        form.beforeContent = '';
        form.afterContent = '';
        form.reason = '';
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '变更发起失败'));
    }
}

async function selectChange(row: ChangeOrder) {
    activeChange.value = row;
    try {
        await impactPagination.reset();
    }
    catch {
        impacts.value = [];
    }
}

async function analyzeImpact(row: ChangeOrder) {
    try {
        activeChange.value = row;
        await api.analyzeChange(row.id);
        await impactPagination.reset();
        ElMessage.success('影响矩阵已生成');
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '影响分析失败'));
    }
}

watch(changeFilter, () => {
    void changePagination.reset().catch((error) => ElMessage.error(userFacingError(error, '变更列表加载失败')));
});

async function approveChange(row: ChangeOrder) {
    try {
        await ElMessageBox.confirm('确认批准此变更？批准后将通知所有影响项责任人。', '审批变更', { type: 'warning' });
        await api.approveChange(row.id);
        ElMessage.success('变更已批准');
        await load();
    }
    catch {
        // 取消审批后保持当前列表状态。
    }
}

async function feedbackChange(row: ChangeOrder) {
    try {
        await api.feedbackChange(row.id, {
            result: '已全部重排并同步责任人',
            plan: '今日完成全部调整',
            delayDays: 2, reworkQty: 0,
            responsibleName: '项目协调员'
        } as any);
        ElMessage.success('执行反馈已提交');
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '反馈提交失败'));
    }
}

async function closeChange(row: ChangeOrder) {
    try {
        await api.closeChange(row.id);
        ElMessage.success('变更已关闭');
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '关闭校验未通过'));
    }
}

onMounted(load);
</script>

<template>
    <!-- 变更查询与处置 -->
    <div class="stack" v-loading="loading">
        <WorkflowStrip
            :steps="[
                { key: 'draft', label: '发起变更', hint: '说明前后差异', state: activeChange?.status === 'DRAFT' ? 'current' : activeChange ? 'done' : 'current' },
                { key: 'impact', label: '影响分析', hint: '汇集部门反馈', state: activeChange?.status === 'WAIT_IMPACT' ? 'current' : activeChange && activeChange.status !== 'DRAFT' ? 'done' : 'pending' },
                { key: 'approval', label: '审批', hint: '确认风险与成本', state: activeChange?.status === 'WAIT_APPROVAL' ? 'current' : activeChange && ['EXECUTING', 'CLOSED'].includes(activeChange.status) ? 'done' : 'pending' },
                { key: 'execute', label: '执行反馈', hint: '落实受影响事项', state: activeChange?.status === 'EXECUTING' ? 'current' : activeChange?.status === 'CLOSED' ? 'done' : 'pending' },
                { key: 'close', label: '关闭', hint: '形成完整记录', state: activeChange?.status === 'CLOSED' ? 'done' : 'pending' }
            ]"
            aria-label="变更闭环流程"
        />
        <!-- 新建变更 -->
        <div v-if="auth.can('change:create')" class="panel">
            <div class="panel-header"><span class="panel-title">发起变更</span></div>
            <div class="panel-body flow-form-grid change-create-form">
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">项目 <em>*</em></span>
                    <el-select v-model="form.projectId" placeholder="选择项目" filterable>
                        <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                    </el-select>
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">变更类型 <em>*</em></span>
                    <el-select v-model="form.changeType">
                        <el-option label="设计变更" value="DESIGN" />
                        <el-option label="材料变更" value="MATERIAL_SPEC" />
                        <el-option label="工艺变更" value="PROCESS" />
                        <el-option label="数量变更" value="QUANTITY" />
                        <el-option label="交期变更" value="DELIVERY" />
                        <el-option label="包装变更" value="PACKAGING" />
                    </el-select>
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">紧急程度</span>
                    <el-select v-model="form.urgency">
                        <el-option label="高风险" value="HIGH" />
                        <el-option label="普通" value="NORMAL" />
                        <el-option label="低风险" value="LOW" />
                    </el-select>
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">变更前内容</span>
                    <el-input v-model="form.beforeContent" placeholder="填写现状或原版本" />
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">变更后内容 <em>*</em></span>
                    <el-input v-model="form.afterContent" placeholder="填写目标内容或新版本" />
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">变更原因</span>
                    <el-input v-model="form.reason" placeholder="说明触发原因" />
                </label>
                <div class="flow-form-actions">
                    <el-button type="primary" @click="createChange">发起变更</el-button>
                </div>
            </div>
        </div>

        <!-- 变更工作区 -->
        <section class="change-workspace">
            <div class="panel change-list-panel">
                <div class="panel-header change-list-header">
                    <span class="panel-title">变更单（{{ changePagination.total }}）</span>
                    <el-radio-group v-model="changeFilter" size="small">
                        <el-radio-button value="">全部</el-radio-button>
                        <el-radio-button value="WAIT_IMPACT">待分析</el-radio-button>
                        <el-radio-button value="WAIT_APPROVAL">待审批</el-radio-button>
                        <el-radio-button value="EXECUTING">执行中</el-radio-button>
                        <el-radio-button value="CLOSED">已关闭</el-radio-button>
                    </el-radio-group>
                </div>
                <!-- 数据表格 -->
                <el-table :data="changes" height="480" highlight-current-row @row-click="selectChange" size="small">
                    <el-table-column prop="changeNo" label="变更单号" width="135" />
                    <el-table-column prop="projectNo" label="项目" width="128" />
                    <el-table-column label="类型" width="88"><template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template></el-table-column>
                    <el-table-column label="状态" width="104">
                        <template #default="{ row }">
                            <el-tag :type="statusTag(row.status)" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="reason" label="变更原因" min-width="160" show-overflow-tooltip />
                    <el-table-column label="反馈" width="56" align="center">
                        <template #default="{ row }">
                            {{ row.feedbackCount || 0 }}/{{ row.impactCount || 0 }}
                        </template>
                    </el-table-column>
                    <el-table-column label="延期" width="56" align="center">
                        <template #default="{ row }">
                            {{ row.delayDays || 0 }}天
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="150">
                        <template #default="{ row }">
                            <el-button v-if="auth.can('change:analyze') && ['WAIT_IMPACT', 'DRAFT'].includes(row.status)" link type="primary" size="small" @click.stop="analyzeImpact(row)">影响分析</el-button>
                            <el-button v-if="auth.can('change:approve') && row.status === 'WAIT_APPROVAL'" link type="warning" size="small" @click.stop="approveChange(row)">审批</el-button>
                            <el-button v-if="auth.can('change:feedback') && row.status === 'EXECUTING'" link type="success" size="small" @click.stop="feedbackChange(row)">执行反馈</el-button>
                            <el-button v-if="auth.can('change:close') && row.status === 'EXECUTING'" link type="danger" size="small" @click.stop="closeChange(row)">关闭</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="changePagination.pageNo"
                    :page-size="changePagination.pageSize"
                    :total="changePagination.total"
                    :loading="changePagination.loading"
                    @update:page-no="changePagination.goTo"
                    @update:page-size="changePagination.changePageSize"
                />
            </div>

            <!-- 影响信息 -->
            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">影响矩阵</span>
                    <span v-if="activeChange" class="subtle">{{ activeChange.changeNo }}</span>
                    <el-tag v-if="activeChange" class="change-status-tag" :type="statusTag(activeChange.status)" effect="dark" size="small">{{ statusLabel(activeChange.status) }}</el-tag>
                </div>
                <div class="panel-body stack change-impact-list">
                    <!-- 内容差异 -->
                    <div v-if="activeChange" class="change-diff">
                        <div class="status-line">
                            <span class="subtle">变更前：</span><span>{{ activeChange.beforeContent || '（未填写）' }}</span>
                        </div>
                        <div class="status-line change-diff__row">
                            <span class="subtle">变更后：</span><strong>{{ activeChange.afterContent }}</strong>
                        </div>
                        <div class="subtle change-diff__row">原因：{{ activeChange.reason || '（未填写）' }}</div>
                    </div>

                    <el-empty v-if="!impacts.length" :description="activeChange ? '当前变更单尚未完成影响分析' : '请先选择左侧变更单，再开始影响分析'" :image-size="80" />
                    <el-button v-if="activeChange && ['DRAFT', 'WAIT_IMPACT'].includes(activeChange.status) && auth.can('change:analyze')" type="primary" @click="analyzeImpact(activeChange)">开始影响分析</el-button>

                    <!-- 影响项 -->
                    <div v-for="item in impacts" :key="item.id" class="change-impact-item">
                        <div class="status-line">
                            <el-tag :type="impactTypeTag(item.objectType)" effect="dark" size="small">{{ impactTypeLabel(item.objectType) }}</el-tag>
                            <strong>{{ item.objectName }}</strong>
                            <el-tag :type="item.status === 'DONE' ? 'success' : 'warning'" effect="plain" size="small">{{ item.status === 'DONE' ? '已完成' : '待反馈' }}</el-tag>
                        </div>
                        <p class="subtle change-impact-item__summary">{{ item.departmentName }} · {{ item.suggestedAction }}</p>
                        <div v-if="item.feedbackResult" class="change-impact-item__feedback">
                            <span class="subtle">反馈：</span><span>{{ item.feedbackResult }}</span>
                            <span v-if="item.responsibleName" class="subtle change-impact-item__owner">· {{ item.responsibleName }}</span>
                        </div>
                    </div>
                    <PaginationBar
                        v-if="activeChange"
                        :page-no="impactPagination.pageNo"
                        :page-size="impactPagination.pageSize"
                        :total="impactPagination.total"
                        :loading="impactPagination.loading"
                        @update:page-no="impactPagination.goTo"
                        @update:page-size="impactPagination.changePageSize"
                    />
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
/* 变更中心局部样式。 */
.change-workspace {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
}

.change-list-panel {
    min-width: 0;
}

.change-list-header {
    flex-wrap: wrap;
    gap: 8px 16px;
    padding-top: 8px;
    padding-bottom: 8px;
}

.change-status-tag {
    margin-left: 8px;
}

.change-impact-list {
    gap: 8px;
}

.change-diff {
    padding: 11px 12px;
    border: 1px solid var(--nso-divider);
    border-radius: var(--nso-radius);
    background: var(--nso-surface-soft);
}

.change-diff__row,
.change-impact-item__feedback {
    margin-top: 4px;
}

.change-impact-item {
    padding: 11px 12px;
    border: 1px solid var(--nso-border);
    border-radius: var(--nso-radius);
    background: #fff;
    transition: border-color var(--nso-ease), box-shadow var(--nso-ease);
}

.change-impact-item:hover {
    border-color: var(--nso-border-strong);
    box-shadow: var(--nso-shadow-sm);
}

.change-impact-item__summary {
    margin: 6px 0 2px;
}

.change-impact-item__owner {
    margin-left: 8px;
}

@media (max-width: 680px) {
    .change-list-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .change-list-header :deep(.el-radio-group) {
        max-width: 100%;
        overflow-x: auto;
    }
}
</style>
