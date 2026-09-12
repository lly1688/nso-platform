<script setup lang="ts">
// 样品计划、检验与确认流程状态。
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { getRuleBlockDetail, userFacingError } from '@/utils/request';
import { openProtectedFile } from '@/utils/file-access';
import { formatChineseDate, formatChineseDateTime } from '@/utils/date';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { ConfirmationToken, ExternalProjectAccess, Project, RuleBlockDetail, Sample, SampleCheck } from '@/types';

interface SampleSubmitBlocker {
    sampleId: number;
    sampleNo: string;
    reason: string;
    missingStep: string;
    responsibleRole: string;
    action: string;
    detail?: RuleBlockDetail;
}

const projects = ref<Project[]>([]);
const samples = ref<Sample[]>([]);
const checks = ref<SampleCheck[]>([]);
const token = ref<ConfirmationToken>();
const activeSample = ref<Sample>();
const loading = ref(false);
const sampleFilter = ref('');
const auth = useAuthStore();
const router = useRouter();
const tokenDialogVisible = ref(false);
const proxyConfirmDialogVisible = ref(false);
const proxyConfirmSaving = ref(false);
const sampleSubmitBlocker = ref<SampleSubmitBlocker>();
const tokenSample = ref<Sample>();
const proxyConfirmSample = ref<Sample>();
const tokenContacts = ref<ExternalProjectAccess[]>([]);
const tokenForm = reactive({ contactId: undefined as number | undefined, validDays: 7, maxUseCount: 1 });
const proxyConfirmForm = reactive({ conclusion: 'PASS', opinion: '', evidence: undefined as File | undefined });
const form = reactive({
    projectId: undefined as number | undefined,
    purpose: '', quantity: 1, planFinishDate: '', referencedVersion: '', responsibleName: ''
});

const checkForm = reactive({
    checkItem: '', measuredValue: '', result: 'PASS', issueSummary: '', correctiveAction: '', checkerName: ''
});

const samplePagination = usePagination<Sample>();
const checkPagination = usePagination<SampleCheck>({ queryPrefix: 'check' });

samplePagination.configure(
    (params) => api.samples({ status: sampleFilter.value || undefined, ...params }),
    (result) => {
        samples.value = result.list;
    }
);

checkPagination.configure(
    (params) => activeSample.value
        ? api.sampleChecks(activeSample.value.id, params)
        : Promise.resolve({ list: [], total: 0, pageNo: params.pageNo, pageSize: params.pageSize }),
    (result) => {
        checks.value = result.list;
    }
);

const statusTag = (status: string) => ({ DRAFT: 'info', REWORKING: 'danger', CHECKED: 'warning', WAIT_CUSTOMER_CONFIRM: 'info', WAIT_SUPPLEMENT: 'warning', CONFIRMED: 'success', CONDITIONAL_PASS: 'warning', REJECTED: 'danger' } as Record<string, string>)[status] || 'info';
const statusLabel = (status: string) => ({ DRAFT: '草稿', REWORKING: '整改中', CHECKED: '已检验', WAIT_CUSTOMER_CONFIRM: '待客户确认', WAIT_SUPPLEMENT: '待补充', CONFIRMED: '已通过', CONDITIONAL_PASS: '条件通过', REJECTED: '已驳回' } as Record<string, string>)[status] || status;
const confirmLabel = (status: string) => ({ PASS: '通过', CONDITIONAL_PASS: '条件通过', WAIT_SUPPLEMENT: '待补充', REJECT: '驳回', PENDING: '待确认' } as Record<string, string>)[status] || status;
const checkResultLabel = (result: string) => ({ PASS: '合格', FAIL: '不合格' } as Record<string, string>)[result] || result;
async function load() {
    loading.value = true;
    try {
        projects.value = ((await api.projects()) as any).list || [];
        await samplePagination.reload();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '数据加载失败'));
    }
    finally {
        loading.value = false;
    }
}

async function createSample() {
    if (!form.projectId) {
        ElMessage.warning('请选择项目');
        return;
    }
    if (!form.purpose.trim()) {
        ElMessage.warning('请填写打样目的');
        return;
    }
    try {
        await api.createSample({ ...form, purpose: form.purpose.trim() });
        ElMessage.success('样品单已创建');
        form.purpose = '';
        form.referencedVersion = '';
        await load();
    }
    catch (error) {
        const message = userFacingError(error, '样品创建失败');
        if (message.includes('SAMPLE_TECHNICAL_PACKAGE')) {
            try {
                await ElMessageBox.confirm('该项目尚未发布完整技术包。现在前往对应项目的技术包检查页？', '样品创建被技术包阻断', {
                    type: 'warning', confirmButtonText: '前往技术包', cancelButtonText: '留在当前页'
                });
                await router.push({ name: 'documents', query: { projectId: String(form.projectId) } });
            }
            catch {
                // 操作人员选择留在当前样品页面。
            }
            return;
        }
        ElMessage.error(message);
    }
}

async function selectSample(row: Sample) {
    activeSample.value = row;
    token.value = undefined;
    try {
        await checkPagination.reset();
    }
    catch {
        checks.value = [];
    }
}

watch(sampleFilter, () => {
    void samplePagination.reset().catch((error) => ElMessage.error(userFacingError(error, '样品列表加载失败')));
});

async function addCheck() {
    if (!activeSample.value) {
        return;
    }
    if (!checkForm.checkItem.trim()) {
        ElMessage.warning('请填写检验项目');
        return;
    }
    try {
        await api.addSampleCheck(activeSample.value.id, { ...checkForm, checkItem: checkForm.checkItem.trim() });
        ElMessage.success('检验记录已保存');
        if (sampleSubmitBlocker.value?.sampleId === activeSample.value.id) {
            sampleSubmitBlocker.value = undefined;
        }
        checkForm.checkItem = '';
        checkForm.measuredValue = '';
        checkForm.issueSummary = '';
        await selectSample(activeSample.value);
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '检验保存失败'));
    }
}

function isConfirmDialogCancel(error: unknown) {
    return error === 'cancel' || error === 'close';
}

function submitBlockerFor(row: Sample, error: unknown): SampleSubmitBlocker {
    const detail = getRuleBlockDetail(error);
    if (detail?.ruleCode === 'SAMPLE_QUALITY_CHECK') {
        return {
            sampleId: row.id,
            sampleNo: row.sampleNo,
            reason: detail.reason || '样品尚未完成独立质量检验',
            missingStep: '独立质量人员完成合格检验并保存检验结论',
            responsibleRole: '质量负责人 / 质量人员',
            action: '由质量人员在样品详情中录入“合格”检验记录；样品制作人员不能代填。完成后由项目经理再次提交客户确认。',
            detail
        };
    }
    if (detail?.ruleCode === 'SAMPLE_TASKS_PENDING') {
        const task = detail.currentValue === 'SAMPLE_MAKE'
            ? { step: '样品制作任务', action: '由生产人员完成“样品制作”任务并反馈完成。' }
            : { step: '样品备料任务', action: '由生产人员完成“样品备料”任务并反馈完成。' };
        return {
            sampleId: row.id,
            sampleNo: row.sampleNo,
            reason: detail.reason || '样品准备任务尚未完成',
            missingStep: task.step,
            responsibleRole: '生产负责人 / 生产人员',
            action: `${task.action} 若系统随后继续提示另一项任务，请完成该项后再由项目经理提交客户确认。`,
            detail
        };
    }
    return {
        sampleId: row.id,
        sampleNo: row.sampleNo,
        reason: detail?.reason || userFacingError(error, '样品暂不满足提交客户确认的条件'),
        missingStep: detail?.currentValue || '请完成当前阻断步骤',
        responsibleRole: '请按项目职责分工处理',
        action: detail?.action || '完成提示的前置步骤后，再提交客户确认。',
        detail
    };
}

async function submitConfirm(row: Sample) {
    sampleSubmitBlocker.value = undefined;
    try {
        await ElMessageBox.confirm('确认提交客户确认？样品将进入"待客户确认"状态。', '提交确认', { type: 'warning' });
        await api.submitSampleConfirm(row.id);
        ElMessage.success('已提交客户确认');
        await load();
    }
    catch (error) {
        if (isConfirmDialogCancel(error)) {
            return;
        }
        const blocker = submitBlockerFor(row, error);
        sampleSubmitBlocker.value = blocker;
        await selectSample(row);
        ElMessage.error(`暂不能提交：还差“${blocker.missingStep}”`);
    }
}

function openProxyConfirm(row: Sample, conclusion: string) {
    proxyConfirmSample.value = row;
    proxyConfirmForm.conclusion = conclusion;
    proxyConfirmForm.opinion = conclusion === 'PASS' ? '客户确认样品通过，可进入生产准备。' : '';
    proxyConfirmForm.evidence = undefined;
    proxyConfirmDialogVisible.value = true;
}

function selectProxyEvidence(event: Event) {
    const target = event.target as HTMLInputElement;
    proxyConfirmForm.evidence = target.files?.[0];
}

async function confirmSample() {
    const row = proxyConfirmSample.value;
    if (!row) {
        return;
    }
    if (!proxyConfirmForm.opinion.trim()) {
        return ElMessage.warning('请填写客户确认意见');
    }
    if (!proxyConfirmForm.evidence) {
        return ElMessage.warning('内部代录必须上传客户确认凭证');
    }
    proxyConfirmSaving.value = true;
    try {
        const evidence = await api.uploadSampleConfirmationEvidence(row.id, proxyConfirmForm.evidence);
        void openProtectedFile(api.fileInlineUrl(evidence.id), { preview: true, fileName: evidence.fileName, contentType: evidence.contentType });
        await api.confirmSample(row.id, { conclusion: proxyConfirmForm.conclusion, opinion: proxyConfirmForm.opinion.trim(), confirmer: '内部代录', evidenceFileId: evidence.id });
        ElMessage.success(proxyConfirmForm.conclusion === 'PASS' ? '样品已通过并已留存凭证' : '客户结论已代录并留存凭证');
        proxyConfirmDialogVisible.value = false;
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '确认操作失败'));
    }
    finally {
        proxyConfirmSaving.value = false;
    }
}

async function createToken(row: Sample) {
    try {
        tokenContacts.value = ((await api.projectCustomerAuthorizations(row.projectId)) as any).list?.filter((item: ExternalProjectAccess) => item.status === 'ACTIVE') || [];
        if (!tokenContacts.value.length) {
            return ElMessage.warning('请先在项目页为客户联系人完成项目授权');
        }
        tokenSample.value = row;
        tokenForm.contactId = tokenContacts.value[0].contactId;
        tokenForm.validDays = 7;
        tokenForm.maxUseCount = 1;
        tokenDialogVisible.value = true;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '令牌生成失败'));
    }
}

async function generateToken() {
    const contactId = tokenForm.contactId;
    if (!tokenSample.value || !contactId) {
        return ElMessage.warning('请选择已授权的客户联系人');
    }
    try {
        token.value = await api.createConfirmToken(tokenSample.value.id, { contactId, validDays: tokenForm.validDays, maxUseCount: tokenForm.maxUseCount }) as ConfirmationToken;
        tokenDialogVisible.value = false;
        ElMessage.success('客户确认链接已生成');
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '确认链接生成失败'));
    }
}

async function revokeTokens(row: Sample) {
    try {
        await ElMessageBox.confirm('撤销后，已发送的客户确认链接将立即失效。', '撤销客户确认授权', { type: 'warning' });
        await api.revokeConfirmTokens(row.id);
        token.value = undefined;
        ElMessage.success('客户确认授权已撤销');
    }
    catch {
        // 关闭令牌确认框后不刷新当前样品。
    }
}

function copyToken() {
    if (!token.value?.token) {
        return;
    }
    navigator.clipboard.writeText(`${window.location.origin}/public/confirm/${token.value.token}`);
    ElMessage.success('确认链接已复制');
}

function handleSampleBlocker() {
    const blocker = sampleSubmitBlocker.value;
    if (!blocker) {
        return;
    }
    if (blocker.detail?.ruleCode === 'SAMPLE_TECHNICAL_PACKAGE') {
        const projectId = activeSample.value?.projectId;
        if (projectId) {
            void router.push({ name: 'documents', query: { projectId: String(projectId) } });
        }
        return;
    }
    if (blocker.detail?.ruleCode === 'SAMPLE_TASKS_PENDING') {
        const projectId = activeSample.value?.projectId;
        void router.push(projectId
            ? { name: 'tasks-execution', query: { projectId: String(projectId) } }
            : { name: 'tasks-execution' });
        return;
    }
    document.getElementById('sample-check-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

onMounted(load);
</script>

<template>
    <!-- 样品查询、检验与确认 -->
    <div class="stack" v-loading="loading">
        <WorkflowStrip
            :steps="[
                { key: 'create', label: '创建样品单', hint: '填写计划与责任人', state: activeSample ? 'done' : 'current' },
                { key: 'inspect', label: '完成检验', hint: '记录检验结果', state: activeSample?.status === 'CHECKED' || activeSample?.status === 'WAIT_CUSTOMER_CONFIRM' || activeSample?.status === 'CONFIRMED' ? 'done' : activeSample ? 'current' : 'pending' },
                { key: 'confirm', label: '客户确认', hint: '提交确认结论', state: activeSample?.status === 'CONFIRMED' ? 'done' : activeSample?.status === 'WAIT_CUSTOMER_CONFIRM' || activeSample?.status === 'CHECKED' ? 'current' : 'pending' },
                { key: 'prepare', label: '生产准备', hint: '进入后续任务', state: activeSample?.status === 'CONFIRMED' ? 'current' : 'pending' }
            ]"
            aria-label="样品确认流程"
        />
        <!-- 新建样品 -->
        <div v-if="auth.can('sample:create')" class="panel">
            <div class="panel-header"><span class="panel-title">样品申请</span></div>
            <div class="panel-body flow-form-grid sample-create-form">
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">项目 <em>*</em></span>
                    <el-select v-model="form.projectId" placeholder="选择项目" filterable>
                        <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                    </el-select>
                </label>
                <label class="flow-field flow-field--span-2">
                    <span class="flow-field__label">打样目的 <em>*</em></span>
                    <el-input v-model="form.purpose" placeholder="例如：验证关键尺寸" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">数量 <em>*</em></span>
                    <el-input-number v-model="form.quantity" :min="1" :controls="false" aria-label="打样数量" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">引用技术版本</span>
                    <el-input v-model="form.referencedVersion" placeholder="例如：V1.0" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">计划完成</span>
                    <el-date-picker v-model="form.planFinishDate" format="YYYY年MM月DD日" value-format="YYYY-MM-DD" placeholder="选择日期" />
                </label>
                <label class="flow-field">
                    <span class="flow-field__label">责任人</span>
                    <el-input v-model="form.responsibleName" placeholder="填写姓名" />
                </label>
                <div class="flow-form-actions">
                    <el-button type="primary" @click="createSample">创建样品单</el-button>
                </div>
            </div>
        </div>

        <el-alert
            v-if="sampleSubmitBlocker"
            class="sample-submit-blocker"
            :title="`${sampleSubmitBlocker.sampleNo} 暂不能提交客户确认`"
            type="error"
            show-icon
            :closable="false"
        >
            <template #default>
                <div class="sample-submit-blocker__details">
                    <span><b>还差步骤</b>{{ sampleSubmitBlocker.missingStep }}</span>
                    <span><b>责任人</b>{{ sampleSubmitBlocker.responsibleRole }}</span>
                    <span><b>处理动作</b>{{ sampleSubmitBlocker.action }}</span>
                    <span class="sample-submit-blocker__reason">系统校验：{{ sampleSubmitBlocker.reason }}</span>
                    <el-button type="primary" link size="small" @click="handleSampleBlocker">{{ sampleSubmitBlocker.detail?.ruleCode === 'SAMPLE_QUALITY_CHECK' ? '回到检验' : sampleSubmitBlocker.detail?.ruleCode === 'SAMPLE_TASKS_PENDING' ? '进入任务' : '去技术包' }}</el-button>
                    <el-button link size="small" @click="sampleSubmitBlocker = undefined">关闭提示</el-button>
                </div>
            </template>
        </el-alert>

        <!-- 样品列表 -->
        <div class="panel">
            <div class="panel-header">
                <span class="panel-title">样品确认闭环（{{ samplePagination.total }}）</span>
                <el-radio-group v-model="sampleFilter" size="small">
                    <el-radio-button value="">全部</el-radio-button>
                    <el-radio-button value="DRAFT">草稿</el-radio-button>
                    <el-radio-button value="CHECKED">已检验</el-radio-button>
                    <el-radio-button value="WAIT_CUSTOMER_CONFIRM">待确认</el-radio-button>
                    <el-radio-button value="CONFIRMED">已通过</el-radio-button>
                    <el-radio-button value="REJECTED">已驳回</el-radio-button>
                </el-radio-group>
            </div>
            <!-- 数据表格 -->
            <el-table :data="samples" height="340" highlight-current-row @row-click="selectSample" size="small">
                <el-table-column prop="sampleNo" label="样品单号" width="160" />
                <el-table-column prop="projectNo" label="项目" width="150" />
                <el-table-column prop="purpose" label="打样目的" min-width="180" show-overflow-tooltip />
                <el-table-column prop="referencedVersion" label="版本" width="90" />
                <el-table-column prop="planFinishDate" label="计划完成" width="145">
                    <template #default="{ row }">{{ formatChineseDate(row.planFinishDate) }}</template>
                </el-table-column>
                <el-table-column prop="responsibleName" label="责任人" width="100" />
                <el-table-column label="状态" width="140">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="结论" width="90">
                    <template #default="{ row }">
                        <span v-if="row.confirmConclusion" class="subtle">{{ confirmLabel(row.confirmConclusion) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="auth.can('sample:submit') && row.status === 'CHECKED'" link type="primary" size="small" @click.stop="submitConfirm(row)">提交确认</el-button>
                        <el-button v-if="auth.can('sample:submit') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="warning" size="small" @click.stop="createToken(row)">生成令牌</el-button>
                        <el-button v-if="auth.can('sample:submit') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="danger" size="small" @click.stop="revokeTokens(row)">撤销授权</el-button>
                        <el-button v-if="auth.can('sample:proxy-confirm') && row.status === 'WAIT_CUSTOMER_CONFIRM'" link type="success" size="small" @click.stop="openProxyConfirm(row, 'PASS')">内部代录</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="samplePagination.pageNo"
                :page-size="samplePagination.pageSize"
                :total="samplePagination.total"
                :loading="samplePagination.loading"
                @update:page-no="samplePagination.goTo"
                @update:page-size="samplePagination.changePageSize"
            />
        </div>

        <!-- 样品详情 -->
        <section v-if="activeSample" class="two-column">
            <div class="panel">
                <div class="panel-header">
                    <span class="panel-title">样品检验：{{ activeSample.sampleNo }}</span>
                    <el-tag :type="statusTag(activeSample.status)" effect="dark" size="small">{{ statusLabel(activeSample.status) }}</el-tag>
                </div>
                <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
                    <el-descriptions-item label="打样目的">{{ activeSample.purpose }}</el-descriptions-item>
                    <el-descriptions-item label="数量">{{ activeSample.quantity }}</el-descriptions-item>
                    <el-descriptions-item label="引用版本">{{ activeSample.referencedVersion || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="责任人">{{ activeSample.responsibleName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="确认结论">{{ confirmLabel(activeSample.confirmConclusion) }}</el-descriptions-item>
                    <el-descriptions-item label="问题摘要">{{ activeSample.issueSummary || '无' }}</el-descriptions-item>
                </el-descriptions>
                <div v-if="auth.can('sample:inspect')" id="sample-check-form" class="panel-body flow-form-grid sample-check-form">
                    <label class="flow-field flow-field--span-2">
                        <span class="flow-field__label">检验项目 <em>*</em></span>
                        <el-input v-model="checkForm.checkItem" placeholder="例如：关键尺寸" />
                    </label>
                    <label class="flow-field">
                        <span class="flow-field__label">实测值</span>
                        <el-input v-model="checkForm.measuredValue" placeholder="填写测量结果" />
                    </label>
                    <label class="flow-field">
                        <span class="flow-field__label">检验结论 <em>*</em></span>
                        <el-select v-model="checkForm.result">
                            <el-option label="合格" value="PASS" />
                            <el-option label="不合格" value="FAIL" />
                        </el-select>
                    </label>
                    <label class="flow-field">
                        <span class="flow-field__label">检验人</span>
                        <el-input v-model="checkForm.checkerName" placeholder="填写姓名" />
                    </label>
                    <div class="flow-form-actions">
                        <el-button type="primary" @click="addCheck">保存检验记录</el-button>
                    </div>
                </div>
                <el-table :data="checks" height="200" size="small">
                    <el-table-column prop="checkItem" label="检验项目" width="140" />
                    <el-table-column prop="measuredValue" label="实测值" min-width="140" />
                    <el-table-column label="结论" width="90"><template #default="{ row }"><el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" effect="plain" size="small">{{ checkResultLabel(row.result) }}</el-tag></template></el-table-column>
                    <el-table-column prop="checkerName" label="检验人" width="100" />
                    <el-table-column prop="checkedAt" label="时间" width="190">
                        <template #default="{ row }">{{ formatChineseDateTime(row.checkedAt) }}</template>
                    </el-table-column>
                </el-table>
                <PaginationBar
                    :page-no="checkPagination.pageNo"
                    :page-size="checkPagination.pageSize"
                    :total="checkPagination.total"
                    :loading="checkPagination.loading"
                    @update:page-no="checkPagination.goTo"
                    @update:page-size="checkPagination.changePageSize"
                />
            </div>

            <div class="panel">
                <div class="panel-header"><span class="panel-title">客户短期确认入口</span></div>
                <div class="panel-body">
                    <el-descriptions :column="1" border size="small">
                        <el-descriptions-item label="令牌">
                            <template v-if="token">
                                <code style="font-size: 12px; word-break: break-all">{{ token.token }}</code>
                            </template>
                            <span v-else class="subtle">选择"待客户确认"样品后生成令牌</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="有效期">{{ formatChineseDateTime(token?.expireAt) }}</el-descriptions-item>
                        <el-descriptions-item label="使用次数">{{ token ? `${token.usedCount || 0}/${token.maxUseCount}` : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="公共路由">
                            <code v-if="token" style="font-size: 12px">/public/confirm/{{ token.token }}</code>
                            <span v-else class="subtle">-</span>
                        </el-descriptions-item>
                    </el-descriptions>
                    <el-button v-if="token" type="primary" size="small" style="margin-top: 12px" @click="copyToken">复制确认链接</el-button>
                </div>
            </div>
        </section>

        <!-- 操作弹窗 -->
        <el-dialog v-model="tokenDialogVisible" title="生成客户样品确认链接" width="min(500px, 92vw)">
            <el-alert title="客户不登录内部平台。链接只对已启用且已获得本项目授权的联系人有效，撤销授权或停用联系人会立即失效。" type="info" :closable="false" style="margin-bottom: 16px" />
            <el-form label-position="top">
                <el-form-item label="客户联系人" required>
                    <el-select v-model="tokenForm.contactId" filterable style="width: 100%">
                        <el-option
                            v-for="access in tokenContacts"
                            :key="access.id"
                            :label="`${access.contactName} · ${access.projectNo}`"
                            :value="access.contactId"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="有效天数"><el-input-number v-model="tokenForm.validDays" :min="1" :max="30" :controls="false" /></el-form-item>
                <el-form-item label="最大使用次数"><el-input-number v-model="tokenForm.maxUseCount" :min="1" :max="5" :controls="false" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="tokenDialogVisible = false">取消</el-button><el-button type="primary" @click="generateToken">生成链接</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="proxyConfirmDialogVisible" title="内部代录客户确认" width="min(560px, 92vw)" :close-on-click-modal="false">
            <el-alert title="内部代录会写入代录人、意见和项目内凭证；客户使用短链确认时不需要内部账号和凭证上传。" type="warning" :closable="false" style="margin-bottom:16px" />
            <el-form label-position="top">
                <el-form-item label="样品">
                    <el-input :model-value="proxyConfirmSample ? `${proxyConfirmSample.sampleNo} · ${proxyConfirmSample.purpose}` : ''" disabled />
                </el-form-item>
                <el-form-item label="客户确认结论" required>
                    <el-select v-model="proxyConfirmForm.conclusion" style="width:100%">
                        <el-option label="通过" value="PASS" />
                        <el-option label="条件通过" value="CONDITIONAL_PASS" />
                        <el-option label="待补充" value="WAIT_SUPPLEMENT" />
                        <el-option label="驳回" value="REJECT" />
                    </el-select>
                </el-form-item>
                <el-form-item label="客户意见" required>
                    <el-input v-model="proxyConfirmForm.opinion" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="如实记录客户意见" />
                </el-form-item>
                <el-form-item label="确认凭证" required>
                    <input type="file" accept=".pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xlsx,.xls" @change="selectProxyEvidence" />
                    <div class="subtle" style="margin-top:6px">{{ proxyConfirmForm.evidence?.name || '请选择客户邮件、签字单或聊天截图等项目内凭证' }}</div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="proxyConfirmDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="proxyConfirmSaving" @click="confirmSample">上传凭证并代录</el-button>
            </template>
        </el-dialog>
    </div>
</template>
