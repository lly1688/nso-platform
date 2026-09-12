<script setup lang="ts">
// 业务规则与字典维护状态。
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { RuleParam } from '@/types';
import { emptyPage, optional, pageList } from './shared';

const loading = ref(false);
const rules = ref<RuleParam[]>([]);
const dictionaries = ref<Array<Record<string, unknown>>>([]);
const ruleForm = reactive({
    ruleCode: 'RISK_SAMPLE_CONFIRM',
    ruleName: '样品确认临期预警',
    params: { daysBefore: 3, score: 25 },
    status: 'DRAFT'
});

const dictForm = reactive({ type: '', code: '', label: '', sortNo: 0 });
const rulePagination = usePagination<RuleParam>();
const dictionaryPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'dict' });
rulePagination.configure(
    (params) => api.rules(params),
    (result) => {
        rules.value = result.list;
    }
);
dictionaryPagination.configure(
    (params) => api.dictionaries(params),
    (result) => {
        dictionaries.value = result.list;
    }
);

async function load() {
    loading.value = true;
    try {
        await Promise.all([rulePagination.reload(), dictionaryPagination.reload()]);
    }
    finally {
        loading.value = false;
    }
}

async function saveRule() {
    await api.saveRule({ ...ruleForm, params: { ...ruleForm.params } });
    ElMessage.success('规则参数版本已保存');
    await load();
}

async function publishRule(row: RuleParam) {
    await api.publishRule(row.id);
    ElMessage.success('规则参数已发布');
    await load();
}

async function saveDictionary() {
    if (!dictForm.type.trim() || !dictForm.code.trim() || !dictForm.label.trim()) {
        return ElMessage.warning('请填写字典类型、编码和名称');
    }
    await api.saveDictionary({ ...dictForm });
    dictForm.type = '';
    dictForm.code = '';
    dictForm.label = '';
    dictForm.sortNo = 0;
    ElMessage.success('字典项已保存');
    await load();
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 规则、字典列表与操作 -->
    <div class="system-page system-panel-grid" v-loading="loading">
        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">规则参数版本</span>
            </div>
            <div class="panel-body system-form">
                <el-input v-model="ruleForm.ruleCode" placeholder="规则编码" />
                <el-input v-model="ruleForm.ruleName" placeholder="规则名称" />
                <el-input-number v-model="ruleForm.params.daysBefore" :min="0" :controls="false" placeholder="预警天数" class="system-form__small" />
                <el-input-number v-model="ruleForm.params.score" :min="0" :controls="false" placeholder="风险分" class="system-form__small" />
                <el-button type="primary" :icon="'DocumentAdd'" @click="saveRule">保存版本</el-button>
            </div>
            <!-- 数据表格 -->
            <el-table :data="rules" height="520" size="small">
                <el-table-column prop="ruleCode" label="规则编码" width="180" />
                <el-table-column prop="ruleName" label="规则名称" min-width="170" />
                <el-table-column prop="ruleVersion" label="版本" width="90" />
                <el-table-column prop="status" label="状态" width="110" />
                <el-table-column label="操作" width="110" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="success" @click="publishRule(row)">发布</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="rulePagination.pageNo"
                :page-size="rulePagination.pageSize"
                :total="rulePagination.total"
                :loading="rulePagination.loading"
                @update:page-no="rulePagination.goTo"
                @update:page-size="rulePagination.changePageSize"
            />
        </section>

        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">字典项</span>
            </div>
            <div class="panel-body system-form">
                <el-input v-model="dictForm.type" placeholder="字典类型" />
                <el-input v-model="dictForm.code" placeholder="字典编码" />
                <el-input v-model="dictForm.label" placeholder="字典名称" />
                <el-input-number v-model="dictForm.sortNo" :min="0" :controls="false" placeholder="排序" class="system-form__small" />
                <el-button type="primary" @click="saveDictionary">保存字典项</el-button>
            </div>
            <el-table :data="dictionaries" height="520" size="small">
                <el-table-column prop="dict_type" label="类型" min-width="120" />
                <el-table-column prop="dict_code" label="编码" min-width="120" />
                <el-table-column prop="dict_label" label="名称" min-width="140" />
                <el-table-column prop="sort_no" label="排序" width="80" />
            </el-table>
            <PaginationBar
                :page-no="dictionaryPagination.pageNo"
                :page-size="dictionaryPagination.pageSize"
                :total="dictionaryPagination.total"
                :loading="dictionaryPagination.loading"
                @update:page-no="dictionaryPagination.goTo"
                @update:page-size="dictionaryPagination.changePageSize"
            />
        </section>
    </div>
</template>
