<script setup lang="ts">
// 技术文件、BOM 与工艺资料状态。
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import { openProtectedFile } from '@/utils/file-access';
import { formatChineseDate } from '@/utils/date';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { Bom, DocumentVersion, InspectionSpec, ProcessRoute, Project } from '@/types';

const projects = ref<Project[]>([]);
const documents = ref<DocumentVersion[]>([]);
const boms = ref<Bom[]>([]);
const routes = ref<ProcessRoute[]>([]);
const specs = ref<InspectionSpec[]>([]);
const activeTab = ref('versions');
const selectedFile = ref<File>();
const fileInput = ref<HTMLInputElement>();
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

function projectIdFromQuery() {
    const raw = Array.isArray(route.query.projectId) ? route.query.projectId[0] : route.query.projectId;
    const projectId = Number(raw);
    return Number.isInteger(projectId) && projectId > 0 ? projectId : undefined;
}

const selectedProjectId = ref<number | undefined>(projectIdFromQuery());
const versionForm = reactive({
    fileName: '', fileType: 'DRAWING', versionNo: '', effectiveDate: '', changeSummary: ''
});

const bomForm = reactive({
    bomNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
});

const routeForm = reactive({
    routeNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
});

const specForm = reactive({
    specNo: '', versionNo: '', boundDocVersionId: undefined as number | undefined
});
const documentPagination = usePagination<DocumentVersion>();
const bomPagination = usePagination<Bom>({ queryPrefix: 'bom' });
const routePagination = usePagination<ProcessRoute>({ queryPrefix: 'route' });
const specPagination = usePagination<InspectionSpec>({ queryPrefix: 'spec' });

documentPagination.configure(
    (params) => api.documents({ projectId: selectedProjectId.value, ...params }),
    (result) => {
        documents.value = result.list;
    }
);
bomPagination.configure(
    (params) => api.boms({ projectId: selectedProjectId.value, ...params }),
    (result) => {
        boms.value = result.list;
    }
);
routePagination.configure(
    (params) => api.processRoutes({ projectId: selectedProjectId.value, ...params }),
    (result) => {
        routes.value = result.list;
    }
);
specPagination.configure(
    (params) => api.inspectionSpecs({ projectId: selectedProjectId.value, ...params }),
    (result) => {
        specs.value = result.list;
    }
);

const workflowGuidance = {
    bom: {
        role: '项目成员中的技术负责人 / 技术人员',
        completion: '完成物料编码、名称、规格、数量、单位及采购/自制属性维护，并绑定当前生效技术版本',
        next: '工艺负责人维护工艺路线（发布完整技术包前必须完成）'
    },
    route: {
        role: '项目成员中的工艺负责人 / 工艺人员',
        completion: '完成工序顺序、作业说明、设备/工装、标准工时和检验点维护，并绑定当前生效技术版本',
        next: '工艺负责人维护检验规范（发布完整技术包前必须完成）'
    },
    spec: {
        role: '项目成员中的工艺负责人 / 工艺人员',
        completion: '完成检验项目、标准值、抽样规则维护，并绑定当前生效技术版本',
        next: '技术负责人审核并发布完整技术包，之后才能进入样品打样'
    }
} as const;

// 未选择项目时展示全局文档列表，不借用其他项目的版本来执行操作或显示状态。
const currentVersion = computed(() => selectedProjectId.value
    ? documents.value.find(d => d.projectId === selectedProjectId.value && d.currentVersion)
    : undefined);
const technicalPackageChecks = computed(() => {
    if (!selectedProjectId.value) {
        return [];
    }
    const versionId = currentVersion.value?.id;
    return [
        { label: '当前技术版本', ready: Boolean(currentVersion.value), pending: '请先发布图纸或技术文件' },
        { label: 'BOM', ready: Boolean(versionId && boms.value.some(item => item.boundDocVersionId === versionId)), pending: '请创建并绑定当前版本' },
        { label: '工艺路线', ready: Boolean(versionId && routes.value.some(item => item.boundDocVersionId === versionId)), pending: '请创建并绑定当前版本' },
        { label: '检验规范', ready: Boolean(versionId && specs.value.some(item => item.boundDocVersionId === versionId)), pending: '请创建并绑定当前版本' }
    ];
});

const technicalPackageReady = computed(() => technicalPackageChecks.value.length > 0
    && technicalPackageChecks.value.every(item => item.ready));
const responsibilitySteps = computed(() => {
    const versionId = currentVersion.value?.id;
    return [
        {
            label: '技术版本',
            role: '技术负责人 / 技术人员',
            complete: '发布图纸或技术文件',
            next: '开始维护 BOM、工艺路线和检验规范',
            ready: Boolean(currentVersion.value)
        },
        {
            label: 'BOM',
            role: workflowGuidance.bom.role,
            complete: workflowGuidance.bom.completion,
            next: workflowGuidance.bom.next,
            ready: Boolean(versionId && boms.value.some(item => item.boundDocVersionId === versionId))
        },
        {
            label: '工艺路线',
            role: workflowGuidance.route.role,
            complete: workflowGuidance.route.completion,
            next: workflowGuidance.route.next,
            ready: Boolean(versionId && routes.value.some(item => item.boundDocVersionId === versionId))
        },
        {
            label: '检验规范',
            role: workflowGuidance.spec.role,
            complete: workflowGuidance.spec.completion,
            next: workflowGuidance.spec.next,
            ready: Boolean(versionId && specs.value.some(item => item.boundDocVersionId === versionId))
        }
    ];
});
const statusTag = (status: string) => ({ DRAFT: 'warning', PUBLISHED: 'success', EFFECTIVE: 'success', VOID: 'danger' } as Record<string, string>)[status] || 'info';
const statusLabel = (status: string) => ({ DRAFT: '草稿', PUBLISHED: '已发布', EFFECTIVE: '生效中', VOID: '已失效' } as Record<string, string>)[status] || status;
const fileTypeLabel = (type: string) => ({ DRAWING: '图纸', MODEL_3D: '3D模型', BOM: 'BOM', PROCESS: '工艺卡', INSPECTION: '检验规范', CUSTOMER: '客户附件' } as Record<string, string>)[type] || type;

async function load() {
    try {
        const requestedProjectId = projectIdFromQuery();
        if (requestedProjectId !== undefined && requestedProjectId !== selectedProjectId.value) {
            selectedProjectId.value = requestedProjectId;
        }
        const projectData = await api.projects({ pageSize: 100 });
        projects.value = projectData.list || [];
        await documentPagination.reload();
        await reloadActiveTable();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '数据加载失败'));
    }
}

async function reloadActiveTable() {
    if (activeTab.value === 'bom') {
        await bomPagination.reload();
    }
    else if (activeTab.value === 'route') {
        await routePagination.reload();
    }
    else if (activeTab.value === 'spec') {
        await specPagination.reload();
    }
}

async function changeProject(projectId?: number) {
    const query = { ...route.query };
    if (projectId) {
        query.projectId = String(projectId);
    }
    else {
        delete query.projectId;
    }
    await router.replace({ query });
    await load();
}

async function createVersion() {
    if (!selectedProjectId.value) {
        ElMessage.warning('请先选择项目');
        return;
    }
    if (!selectedFile.value || !versionForm.versionNo.trim()) {
        ElMessage.warning('请选择技术文件并填写版本号');
        return;
    }
    try {
        const file = await api.uploadDocumentFile(selectedProjectId.value, selectedFile.value);
        await api.createDocument(selectedProjectId.value, { ...versionForm, fileName: file.fileName, fileObjectId: file.id, versionNo: versionForm.versionNo.trim() });
        ElMessage.success('技术版本已上传为草稿');
        void openProtectedFile(api.fileInlineUrl(file.id), { preview: true, fileName: file.fileName, contentType: file.contentType });
        selectedFile.value = undefined;
        if (fileInput.value) {
            fileInput.value.value = '';
        }
        versionForm.fileName = '';
        versionForm.versionNo = '';
        versionForm.changeSummary = '';
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '版本创建失败'));
    }
}

async function viewDocument(row: DocumentVersion) {
    if (!row.fileObjectId) {
        ElMessage.warning('当前文件内容不可用');
        return;
    }
    try {
        await openProtectedFile(api.documentInlineUrl(row.id), { preview: true, fileName: row.fileName });
    } catch (error) {
        ElMessage.error(userFacingError(error, '文件查看失败'));
    }
}

async function downloadDocument(row: DocumentVersion) {
    if (!row.fileObjectId) {
        ElMessage.warning('当前文件内容不可用');
        return;
    }
    try {
        await openProtectedFile(api.documentDownloadUrl(row.id), { preview: false, fileName: row.fileName });
    } catch (error) {
        ElMessage.error(userFacingError(error, '文件下载失败'));
    }
}

function selectVersionFile(event: Event) {
    const input = event.target as HTMLInputElement;
    selectedFile.value = input.files?.[0];
    versionForm.fileName = selectedFile.value?.name || '';
}

async function publish(row: DocumentVersion) {
    try {
        await ElMessageBox.confirm(`确认发布版本 ${row.versionNo}？发布后旧版本将自动失效。`, '发布确认', { type: 'warning' });
        await api.publishDocument(row.id);
        ElMessage.success(`版本 ${row.versionNo} 已发布`);
        await load();
    }
    catch (error) {
        // 确认框取消时会抛出这些值，真正的接口异常仍需提示给操作人员。
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '发布版本失败'));
        }
    }
}

async function createBom() {
    if (!selectedProjectId.value || !currentVersion.value) {
        ElMessage.warning('请先发布技术版本');
        return;
    }
    try {
        await api.createBom({
            projectId: selectedProjectId.value,
            bomNo: bomForm.bomNo || `BOM-${currentVersion.value.projectNo}`,
            versionNo: bomForm.versionNo || currentVersion.value.versionNo,
            boundDocVersionId: currentVersion.value.id,
            items: [{ materialCode: 'MAT-001', materialName: '关键物料', specification: '按图纸', quantity: 1, unit: '件', sourceType: 'PURCHASE' }]
        });
        ElMessage.success('BOM 已创建');
        bomForm.bomNo = '';
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, 'BOM创建失败'));
    }
}

async function createRoute() {
    if (!selectedProjectId.value || !currentVersion.value) {
        ElMessage.warning('请先发布技术版本');
        return;
    }
    try {
        await api.createProcessRoute({
            projectId: selectedProjectId.value,
            routeNo: routeForm.routeNo || `PROC-${currentVersion.value.projectNo}`,
            versionNo: routeForm.versionNo || currentVersion.value.versionNo,
            boundDocVersionId: currentVersion.value.id,
            steps: [{ stepNo: 10, stepName: '加工', workInstruction: '按技术版本执行', equipmentName: '通用设备', standardHours: 1, outsourceFlag: false, inspectionPoint: true }]
        });
        ElMessage.success('工艺路线已创建');
        routeForm.routeNo = '';
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '工艺路线创建失败'));
    }
}

async function createSpec() {
    if (!selectedProjectId.value || !currentVersion.value) {
        ElMessage.warning('请先发布技术版本');
        return;
    }
    try {
        await api.createInspectionSpec({
            projectId: selectedProjectId.value,
            specNo: specForm.specNo || `QC-${currentVersion.value.projectNo}`,
            versionNo: specForm.versionNo || currentVersion.value.versionNo,
            boundDocVersionId: currentVersion.value.id,
            items: [{ itemName: '关键尺寸', standardValue: '符合图纸', samplingRule: '首件全检' }]
        });
        ElMessage.success('检验规范已创建');
        specForm.specNo = '';
        await load();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '检验规范创建失败'));
    }
}

async function publishPackage() {
    if (!selectedProjectId.value) {
        return ElMessage.warning('请先选择项目');
    }
    if (!technicalPackageReady.value) {
        return ElMessage.warning('请先补齐并绑定当前版本的 BOM、工艺路线和检验规范');
    }
    try {
        await ElMessageBox.confirm('确认发布完整技术包？将校验BOM、工艺、检验规范完整性。', '发布技术包', { type: 'warning' });
        await api.publishTechnicalPackage(selectedProjectId.value);
        ElMessage.success('完整技术包已发布');
        await load();
    }
    catch (error) {
        if (error !== 'cancel') {
            ElMessage.error(userFacingError(error, '技术包发布校验未通过'));
        }
    }
}

onMounted(load);
</script>

<template>
    <!-- 技术资料操作与列表 -->
    <div class="stack">
        <WorkflowStrip
            :steps="[
                { key: 'project', label: '选择项目', hint: '确定维护对象', state: selectedProjectId ? 'done' : 'current' },
                { key: 'version', label: '技术版本', hint: '上传并发布', state: currentVersion ? 'done' : selectedProjectId ? 'current' : 'pending' },
                { key: 'bom', label: 'BOM', hint: '绑定当前版本', state: technicalPackageChecks[1]?.ready ? 'done' : currentVersion ? 'current' : 'pending' },
                { key: 'route', label: '工艺路线', hint: '完善工序', state: technicalPackageChecks[2]?.ready ? 'done' : technicalPackageChecks[1]?.ready ? 'current' : 'pending' },
                { key: 'spec', label: '检验规范', hint: '完成后发布', state: technicalPackageChecks[3]?.ready ? 'done' : technicalPackageChecks[2]?.ready ? 'current' : 'pending' }
            ]"
            aria-label="技术包维护流程"
        />
        <!-- 操作区 -->
            <div class="toolbar">
                <div class="toolbar-left">
                <label class="document-project-field">
                    <span>当前维护项目</span>
                    <el-select v-model="selectedProjectId" placeholder="选择项目" clearable style="width: 320px" @change="changeProject">
                        <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                    </el-select>
                </label>
                <el-tag v-if="currentVersion" type="success" effect="plain">当前生效版本：{{ currentVersion.versionNo }}</el-tag>
                <el-tag v-else-if="selectedProjectId" type="warning" effect="plain">无生效版本</el-tag>
            </div>
        </div>

        <el-alert v-if="!selectedProjectId" title="请选择项目后维护和发布完整技术包"
            description="当前列表展示的是可见项目的汇总，不能据此发布技术包；从样品阻断提示进入时会自动定位对应项目。"
        type="warning" :closable="false" />

        <div v-else class="panel">
            <div class="panel-header"><span class="panel-title">完整技术包检查</span></div>
            <div class="panel-body inline-form">
                <el-tag v-for="item in technicalPackageChecks" :key="item.label"
                    :type="item.ready ? 'success' : 'warning'" effect="plain">
                    {{ item.label }}：{{ item.ready ? '已绑定当前版本' : item.pending }}
                </el-tag>
            </div>
            <div class="panel-body technical-responsibility">
                <div class="technical-responsibility__heading">
                    <strong>责任链与下一步</strong>
                    <span>每一项由对应岗位完成并绑定当前技术版本，技术负责人审核发布后才能进入样品阶段。</span>
                </div>
                <div class="technical-responsibility__steps">
                    <div v-for="(step, index) in responsibilitySteps" :key="step.label" class="technical-responsibility__step">
                        <div class="technical-responsibility__step-head">
                            <span class="technical-responsibility__index">{{ index + 1 }}</span>
                            <strong>{{ step.label }}</strong>
                            <el-tag :type="step.ready ? 'success' : 'warning'" effect="plain" size="small">{{ step.ready ? '已完成' : '待完成' }}</el-tag>
                        </div>
                        <div class="technical-responsibility__row"><span>由谁完成</span><b>{{ step.role }}</b></div>
                        <div class="technical-responsibility__row"><span>完成后</span><em>{{ step.complete }}</em></div>
                        <div class="technical-responsibility__row"><span>下一步</span><em>{{ step.next }}</em></div>
                    </div>
                </div>
            </div>
            <div v-if="!auth.can('document:publish')" class="panel-body">
                <el-alert title="当前账号仅可查看技术资料"
                    description="请切换到已分配该项目技术职责且具备发布权限的账号，完成完整技术包发布后再创建样品。"
                type="info" :closable="false" />
            </div>
        </div>

        <el-tabs v-model="activeTab" @tab-change="reloadActiveTable">
            <el-tab-pane label="技术文件版本" name="versions">
                <div v-if="auth.can('document:upload')" class="panel">
                    <div class="panel-header"><span class="panel-title">上传新版本</span></div>
                    <div class="panel-body flow-form-grid">
                        <label class="flow-field flow-field--span-2">
                            <span class="flow-field__label">技术文件 <em>*</em></span>
                            <input
                                ref="fileInput"
                                type="file"
                                accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.doc,.docx,.dwg,.dxf,.step,.stp,.zip"
                                @change="selectVersionFile"
                            />
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">文件类型 <em>*</em></span>
                            <el-select v-model="versionForm.fileType">
                                <el-option label="图纸" value="DRAWING" />
                                <el-option label="3D模型" value="MODEL_3D" />
                                <el-option label="BOM" value="BOM" />
                                <el-option label="工艺卡" value="PROCESS" />
                                <el-option label="检验规范" value="INSPECTION" />
                                <el-option label="客户附件" value="CUSTOMER" />
                            </el-select>
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">版本号 <em>*</em></span>
                            <el-input v-model="versionForm.versionNo" placeholder="例如：V1.0" />
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">生效日期</span>
                            <el-date-picker v-model="versionForm.effectiveDate" format="YYYY年MM月DD日" value-format="YYYY-MM-DD" placeholder="选择日期" />
                        </label>
                        <label class="flow-field flow-field--span-2">
                            <span class="flow-field__label">变更说明</span>
                            <el-input v-model="versionForm.changeSummary" placeholder="说明本次版本变化" />
                        </label>
                        <div class="flow-form-actions">
                            <el-button type="primary" @click="createVersion">上传为草稿</el-button>
                        </div>
                    </div>
                </div>
                <div class="panel">
                    <div class="panel-header">
                        <span class="panel-title">版本列表</span>
                        <el-button v-if="auth.can('document:publish')" type="success" size="small" :disabled="!technicalPackageReady" @click="publishPackage">发布完整技术包</el-button>
                    </div>
                    <!-- 数据表格 -->
                    <el-table :data="documents" height="400" size="small">
                        <el-table-column prop="projectNo" label="项目" width="150" />
                        <el-table-column prop="fileName" label="文件" min-width="180" show-overflow-tooltip />
                         <el-table-column label="类型" width="100"><template #default="{ row }">{{ fileTypeLabel(row.fileType) }}</template></el-table-column>
                        <el-table-column prop="versionNo" label="版本" width="90" />
                        <el-table-column prop="effectiveDate" label="生效日期" width="145">
                            <template #default="{ row }">{{ formatChineseDate(row.effectiveDate) }}</template>
                        </el-table-column>
                        <el-table-column prop="changeSummary" label="变更说明" min-width="200" show-overflow-tooltip />
                        <el-table-column label="状态" width="110">
                            <template #default="{ row }">
                                <el-tag :type="statusTag(row.status)" effect="plain" size="small">
                                     {{ row.currentVersion ? '生效中' : statusLabel(row.status) }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="180" fixed="right">
                            <template #default="{ row }">
                                <el-button link type="primary" size="small" :disabled="!row.fileObjectId" @click="viewDocument(row)">查看</el-button>
                                <el-button link type="primary" size="small" :disabled="!row.fileObjectId" @click="downloadDocument(row)">下载</el-button>
                                <el-button v-if="auth.can('document:publish')" link type="primary" size="small" :disabled="row.currentVersion" @click="publish(row)">发布</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                    <PaginationBar
                        :page-no="documentPagination.pageNo"
                        :page-size="documentPagination.pageSize"
                        :total="documentPagination.total"
                        :loading="documentPagination.loading"
                        @update:page-no="documentPagination.goTo"
                        @update:page-size="documentPagination.changePageSize"
                    />
                </div>
            </el-tab-pane>

            <el-tab-pane label="BOM" name="bom">
                <div class="document-tab-content">
                    <div class="technical-guidance">
                        <div class="technical-guidance__title"><span class="technical-guidance__step">01</span><strong>BOM 由技术负责人 / 技术人员完成</strong></div>
                        <div class="technical-guidance__grid">
                            <span><b>完成标准</b>{{ workflowGuidance.bom.completion }}</span>
                            <span><b>下一步</b>{{ workflowGuidance.bom.next }}</span>
                        </div>
                    </div>
                    <div v-if="auth.can('bom:manage')" class="panel-body flow-form-grid">
                        <label class="flow-field flow-field--span-2">
                            <span class="flow-field__label">BOM 编号</span>
                            <el-input v-model="bomForm.bomNo" placeholder="留空则按项目自动生成" />
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">绑定版本</span>
                            <el-input v-model="bomForm.versionNo" placeholder="默认当前生效版本" />
                        </label>
                        <div class="flow-form-actions">
                            <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createBom">创建 BOM</el-button>
                        </div>
                    </div>
                    <el-table :data="boms" height="420" size="small">
                        <el-table-column prop="bomNo" label="BOM编号" min-width="180" />
                        <el-table-column prop="versionNo" label="版本" min-width="100" />
                        <el-table-column prop="status" label="状态" min-width="110">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="物料数" min-width="90">
                            <template #default="{ row }">
                                {{ row.items?.length || 0 }}
                            </template>
                        </el-table-column>
                    </el-table>
                    <PaginationBar
                        :page-no="bomPagination.pageNo"
                        :page-size="bomPagination.pageSize"
                        :total="bomPagination.total"
                        :loading="bomPagination.loading"
                        @update:page-no="bomPagination.goTo"
                        @update:page-size="bomPagination.changePageSize"
                    />
                </div>
            </el-tab-pane>

            <el-tab-pane label="工艺路线" name="route">
                <div class="document-tab-content">
                    <div class="technical-guidance">
                        <div class="technical-guidance__title"><span class="technical-guidance__step">02</span><strong>工艺路线由工艺负责人 / 工艺人员完成</strong></div>
                        <div class="technical-guidance__grid">
                            <span><b>完成标准</b>{{ workflowGuidance.route.completion }}</span>
                            <span><b>下一步</b>{{ workflowGuidance.route.next }}</span>
                        </div>
                    </div>
                    <div v-if="auth.can('process:manage')" class="panel-body flow-form-grid">
                        <label class="flow-field flow-field--span-2">
                            <span class="flow-field__label">路线编号</span>
                            <el-input v-model="routeForm.routeNo" placeholder="留空则按项目自动生成" />
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">绑定版本</span>
                            <el-input v-model="routeForm.versionNo" placeholder="默认当前生效版本" />
                        </label>
                        <div class="flow-form-actions">
                            <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createRoute">创建工艺路线</el-button>
                        </div>
                    </div>
                    <el-table :data="routes" height="420" size="small">
                        <el-table-column prop="routeNo" label="路线编号" min-width="180" />
                        <el-table-column prop="versionNo" label="版本" min-width="100" />
                        <el-table-column prop="status" label="状态" min-width="110">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="工序数" min-width="90">
                            <template #default="{ row }">
                                {{ row.steps?.length || 0 }}
                            </template>
                        </el-table-column>
                    </el-table>
                    <PaginationBar
                        :page-no="routePagination.pageNo"
                        :page-size="routePagination.pageSize"
                        :total="routePagination.total"
                        :loading="routePagination.loading"
                        @update:page-no="routePagination.goTo"
                        @update:page-size="routePagination.changePageSize"
                    />
                </div>
            </el-tab-pane>

            <el-tab-pane label="检验规范" name="spec">
                <div class="document-tab-content">
                    <div class="technical-guidance">
                        <div class="technical-guidance__title"><span class="technical-guidance__step">03</span><strong>检验规范由工艺负责人 / 工艺人员完成</strong></div>
                        <div class="technical-guidance__grid">
                            <span><b>完成标准</b>{{ workflowGuidance.spec.completion }}</span>
                            <span><b>下一步</b>{{ workflowGuidance.spec.next }}</span>
                        </div>
                    </div>
                    <div v-if="auth.can('inspection:manage')" class="panel-body flow-form-grid">
                        <label class="flow-field flow-field--span-2">
                            <span class="flow-field__label">规范编号</span>
                            <el-input v-model="specForm.specNo" placeholder="留空则按项目自动生成" />
                        </label>
                        <label class="flow-field">
                            <span class="flow-field__label">绑定版本</span>
                            <el-input v-model="specForm.versionNo" placeholder="默认当前生效版本" />
                        </label>
                        <div class="flow-form-actions">
                            <el-button type="primary" :disabled="!selectedProjectId || !currentVersion" @click="createSpec">创建检验规范</el-button>
                        </div>
                    </div>
                    <el-table :data="specs" height="420" size="small">
                        <el-table-column prop="specNo" label="规范编号" min-width="180" />
                        <el-table-column prop="versionNo" label="版本" min-width="100" />
                        <el-table-column prop="status" label="状态" min-width="110">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain" size="small">{{ statusLabel(row.status) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="检验项" min-width="90">
                            <template #default="{ row }">
                                {{ row.items?.length || 0 }}
                            </template>
                        </el-table-column>
                    </el-table>
                    <PaginationBar
                        :page-no="specPagination.pageNo"
                        :page-size="specPagination.pageSize"
                        :total="specPagination.total"
                        :loading="specPagination.loading"
                        @update:page-no="specPagination.goTo"
                        @update:page-size="specPagination.changePageSize"
                    />
                </div>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>
