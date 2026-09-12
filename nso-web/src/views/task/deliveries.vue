<script setup lang="ts">
// 交付预检、发货与客户反馈状态。
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';
import { useAuthStore } from '@/stores/auth';
import { userFacingError } from '@/utils/request';
import PaginationBar from '@/components/PaginationBar.vue';
import WorkflowStrip from '@/components/WorkflowStrip.vue';
import { usePagination } from '@/composables/usePagination';
import type { DeliveryReadiness, Project } from '@/types';
import { pageList } from './shared';

const auth = useAuthStore();
const projects = ref<Project[]>([]);
const deliveryRows = ref<Record<string, unknown>[]>([]);
const selectedProjectId = ref<number>();
const deliveryReadiness = ref<DeliveryReadiness>();
const loading = ref(false);
const readinessLoading = ref(false);
const deliveryDialogVisible = ref(false);
const deliverySaving = ref(false);
const deliveryForm = reactive({
    quantity: 1,
    logisticsNo: '',
    receiver: '',
    feedback: ''
});
const deliveryStatusLabel = (status: string) => ({ SHIPPED: '已发货', DELIVERED: '已签收', PENDING_DELIVERY: '待交付' } as Record<string, string>)[status] || status;
const deliveryPagination = usePagination<Record<string, unknown>>();
deliveryPagination.configure(
    (params) => api.deliveries(params),
    (result) => {
        deliveryRows.value = result.list;
    }
);

async function fetchDeliveries() {
    await deliveryPagination.reload();
}

async function refreshDeliveries() {
    try {
        await fetchDeliveries();
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '交付记录刷新失败'));
    }
}

async function loadInitialData() {
    loading.value = true;
    try {
        const [, projectData] = await Promise.all([
            deliveryPagination.reload(),
            auth.can('task:execute') ? api.projects({ pageSize: 100 }) : Promise.resolve(undefined)
        ]);
        projects.value = pageList(projectData);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '交付数据加载失败'));
    }
    finally {
        loading.value = false;
    }
}

async function refreshDeliveryReadiness(): Promise<boolean> {
    if (!selectedProjectId.value) {
        return false;
    }
    readinessLoading.value = true;
    try {
        deliveryReadiness.value = await api.deliveryReadiness(selectedProjectId.value);
        return true;
    }
    catch (error) {
        deliveryReadiness.value = undefined;
        ElMessage.error(userFacingError(error, '交付预检失败'));
        return false;
    }
    finally {
        readinessLoading.value = false;
    }
}

async function openDeliveryCheck() {
    if (!selectedProjectId.value) {
        ElMessage.warning('请选择项目');
        return;
    }
    const project = projects.value.find((item) => item.id === selectedProjectId.value);
    deliveryForm.quantity = project?.quantity || 1;
    deliveryForm.logisticsNo = '';
    deliveryForm.receiver = '';
    deliveryForm.feedback = '';
    deliveryReadiness.value = undefined;
    if (await refreshDeliveryReadiness()) {
        deliveryDialogVisible.value = true;
    }
}

async function submitDelivery() {
    if (!selectedProjectId.value) {
        return;
    }
    if (!deliveryForm.logisticsNo.trim() || !deliveryForm.receiver.trim()) {
        ElMessage.warning('请如实填写物流单号和收货人');
        return;
    }
    deliverySaving.value = true;
    try {
        const checked = await refreshDeliveryReadiness();
        if (!checked) {
            return;
        }
        if (!deliveryReadiness.value?.ready) {
            ElMessage.warning('交付预检未通过，请先处理全部阻断项');
            return;
        }
        await ElMessageBox.confirm('确认以当前真实信息创建交付记录？', '交付确认', { type: 'warning' });
        await api.createDelivery({
            projectId: selectedProjectId.value,
            ...deliveryForm,
            logisticsNo: deliveryForm.logisticsNo.trim(),
            receiver: deliveryForm.receiver.trim(),
            feedback: deliveryForm.feedback.trim()
        });
        ElMessage.success('交付记录已创建，项目已进入待交付');
        deliveryDialogVisible.value = false;
        await refreshDeliveries();
    }
    catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(userFacingError(error, '交付检查未通过'));
        }
    }
    finally {
        deliverySaving.value = false;
    }
}

onMounted(() => {
    void loadInitialData();
});
</script>

<template>
    <!-- 交付预检、记录与弹窗 -->
    <div class="module-page" v-loading="loading">
        <WorkflowStrip
            :steps="[
                { key: 'project', label: '选择项目', hint: '确认交付对象', state: selectedProjectId ? 'done' : 'current' },
                { key: 'check', label: '开始预检', hint: '处理阻断项', state: deliveryReadiness ? (deliveryReadiness.ready ? 'done' : 'blocked') : selectedProjectId ? 'current' : 'pending' },
                { key: 'record', label: '填写交付', hint: '记录数量与日期', state: deliveryReadiness?.ready ? 'current' : 'pending' },
                { key: 'confirm', label: '确认提交', hint: '形成交付记录' }
            ]"
            aria-label="交付管理流程"
        />
        <section v-if="auth.can('task:execute')" class="panel">
            <div class="panel-header">
                <span class="panel-title">交付预检</span>
            </div>
            <div class="panel-body task-delivery-toolbar">
                <el-select v-model="selectedProjectId" placeholder="选择待交付项目" filterable>
                    <el-option v-for="item in projects" :key="item.id" :label="`${item.projectNo}  ${item.productName}`" :value="item.id" />
                </el-select>
                <el-button type="primary" :loading="readinessLoading" @click="openDeliveryCheck">开始预检</el-button>
            </div>
        </section>

        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">交付记录（{{ deliveryPagination.total }}）</span>
            </div>
            <!-- 数据表格 -->
            <el-table :data="deliveryRows" height="520" size="small">
                <el-table-column prop="projectNo" label="项目" width="180" />
                <el-table-column prop="quantity" label="交付数量" width="100" />
                <el-table-column prop="logisticsNo" label="物流单号" min-width="200" />
                <el-table-column prop="receiver" label="签收方" width="160" />
                <el-table-column prop="feedback" label="客户反馈" min-width="220" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'SHIPPED' ? 'success' : 'warning'" effect="plain" size="small">{{ deliveryStatusLabel(String(row.status || '')) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="shippedAt" label="发货时间" width="180" />
            </el-table>
            <PaginationBar
                :page-no="deliveryPagination.pageNo"
                :page-size="deliveryPagination.pageSize"
                :total="deliveryPagination.total"
                :loading="deliveryPagination.loading"
                @update:page-no="deliveryPagination.goTo"
                @update:page-size="deliveryPagination.changePageSize"
            />
        </section>

        <!-- 操作弹窗 -->
        <el-dialog
            v-model="deliveryDialogVisible"
            title="交付预检与真实交付记录"
            width="min(620px, 92vw)"
            :close-on-click-modal="false"
            >
            <div v-loading="readinessLoading">
                <el-alert
                    v-if="deliveryReadiness"
                    class="task-readiness-alert"
                    :title="deliveryReadiness.ready ? '预检通过：提交时将再次校验。' : '预检未通过：请处理全部阻断项。'"
                    :type="deliveryReadiness.ready ? 'success' : 'error'"
                    :closable="false"
                />
                <ul v-if="deliveryReadiness?.blockers?.length" class="task-blocker-list">
                    <li v-for="blocker in deliveryReadiness.blockers" :key="blocker.code">
                        <strong>{{ blocker.code }}</strong>
                        <span>{{ blocker.message }}</span>
                    </li>
                </ul>
                <el-form label-position="top">
                    <el-form-item label="交付数量" required>
                        <el-input-number v-model="deliveryForm.quantity" :min="1" :controls="false" class="task-full-control" />
                    </el-form-item>
                    <el-form-item label="物流单号" required>
                        <el-input v-model="deliveryForm.logisticsNo" maxlength="128" placeholder="填写实际物流单号" />
                    </el-form-item>
                    <el-form-item label="收货人" required>
                        <el-input v-model="deliveryForm.receiver" maxlength="128" placeholder="填写实际收货人" />
                    </el-form-item>
                    <el-form-item label="客户反馈">
                        <el-input v-model="deliveryForm.feedback" type="textarea" :rows="3" maxlength="500" placeholder="填写实际反馈" />
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <el-button @click="deliveryDialogVisible = false">取消</el-button>
                <el-button plain :loading="readinessLoading" @click="refreshDeliveryReadiness">重新预检</el-button>
                <el-button type="primary" :loading="deliverySaving" :disabled="!deliveryReadiness?.ready" @click="submitDelivery">创建交付记录</el-button>
            </template>
        </el-dialog>
    </div>
</template>
