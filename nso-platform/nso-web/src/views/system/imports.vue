<script setup lang="ts">
// 数据导入任务与结果状态。
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { ImportTask } from '@/types';
import { emptyPage, optional, pageList } from './shared';
import { formatChineseDateTime } from '@/utils/date';

const loading = ref(false);
const imports = ref<ImportTask[]>([]);
const importRows = ref('[{"name":"测试客户","industry":"非标制造","contactName":"联系人","phone":"13800000000"}]');
const importPagination = usePagination<ImportTask>();
importPagination.configure(
    (params) => api.imports(params),
    (result) => {
        imports.value = result.list;
    }
);

async function load() {
    loading.value = true;
    try {
        await importPagination.reload();
    }
    finally {
        loading.value = false;
    }
}

async function importCustomerRows() {
    try {
        await api.importRows({ importType: 'CUSTOMER', mode: 'INSERT_ONLY', rows: JSON.parse(importRows.value) });
        ElMessage.success('导入任务已执行');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '导入数据格式错误');
    }
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 导入操作与任务记录 -->
    <div class="system-page" v-loading="loading">
        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">客户数据导入</span>
            </div>
            <div class="panel-body stack">
                <el-input v-model="importRows" type="textarea" :rows="8" class="system-textarea" />
                <!-- 操作区 -->
                <div class="toolbar">
                    <span class="subtle">导入类型：CUSTOMER，模式：INSERT_ONLY</span>
                    <el-button type="primary" :icon="'Upload'" @click="importCustomerRows">导入客户</el-button>
                </div>
            </div>
        </section>

        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">导入任务</span>
            </div>
            <!-- 数据表格 -->
            <el-table :data="imports" height="420" size="small">
                <el-table-column prop="importType" label="类型" width="120" />
                <el-table-column prop="mode" label="模式" width="140" />
                <el-table-column prop="status" label="状态" width="120" />
                <el-table-column prop="successCount" label="成功" width="100" />
                <el-table-column prop="failCount" label="失败" width="100" />
                <el-table-column prop="createdAt" label="创建时间" min-width="190" show-overflow-tooltip>
                    <template #default="{ row }">{{ formatChineseDateTime(row.createdAt) }}</template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="importPagination.pageNo"
                :page-size="importPagination.pageSize"
                :total="importPagination.total"
                :loading="importPagination.loading"
                @update:page-no="importPagination.goTo"
                @update:page-size="importPagination.changePageSize"
            />
        </section>
    </div>
</template>
