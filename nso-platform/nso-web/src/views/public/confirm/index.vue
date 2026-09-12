<script setup lang="ts">
// 客户确认令牌与提交状态。
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import type { Sample } from '@/types';

const route = useRoute();
const loading = ref(false);
const sample = ref<Sample>();
const form = reactive({
    conclusion: 'PASS',
    opinion: '',
    confirmer: ''
});

const token = String(route.params.token || '');

async function load() {
    loading.value = true;
    try {
        sample.value = await api.publicConfirmation(token);
    }
    finally {
        loading.value = false;
    }
}

async function submit() {
    if (!form.confirmer.trim() || !form.opinion.trim()) {
        ElMessage.warning('请填写确认人和确认意见');
        return;
    }
    sample.value = await api.submitPublicConfirmation(token, form);
    ElMessage.success('确认结论已提交');
}

onMounted(load);
</script>

<template>
    <!-- 客户确认表单 -->
    <main class="public-page">
        <section class="public-confirm">
            <div class="public-title">
                <h1>样品确认</h1>
                <p>{{ sample?.projectNo || '读取中' }}</p>
            </div>

            <el-skeleton v-if="loading" :rows="5" animated />
            <template v-else>
                <div class="confirm-summary">
                    <div>
                        <span>样品单号</span>
                        <strong>{{ sample?.sampleNo }}</strong>
                    </div>
                    <div>
                        <span>引用版本</span>
                        <strong>{{ sample?.referencedVersion }}</strong>
                    </div>
                    <div>
                        <span>当前状态</span>
                        <strong>{{ sample?.status }}</strong>
                    </div>
                </div>

                <el-form label-position="top" class="confirm-form">
                    <el-form-item label="确认结论">
                        <el-radio-group v-model="form.conclusion">
                            <el-radio-button label="PASS">通过</el-radio-button>
                            <el-radio-button label="CONDITIONAL_PASS">条件通过</el-radio-button>
                            <el-radio-button label="WAIT_SUPPLEMENT">待补充</el-radio-button>
                            <el-radio-button label="REJECT">驳回</el-radio-button>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="确认人">
                        <el-input v-model="form.confirmer" placeholder="姓名/单位" />
                    </el-form-item>
                    <el-form-item label="确认意见">
                        <el-input v-model="form.opinion" type="textarea" :rows="4" placeholder="填写意见或整改要求" />
                    </el-form-item>
                    <el-button type="primary" :disabled="sample?.status !== 'WAIT_CUSTOMER_CONFIRM'" @click="submit">提交确认</el-button>
                </el-form>
            </template>
        </section>
    </main>
</template>
