<script setup lang="ts">
// 工单表单与附件上传状态。
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import type { SupportTicket } from '@/types';
import { userFacingError } from '@/utils/request';

const props = withDefaults(defineProps<{
    modelValue: boolean;
    authenticated?: boolean;
    context?: string;
}>(), {
    authenticated: false,
    context: ''
});

const emit = defineEmits<{
    'update:modelValue': [value: boolean];
    submitted: [ticket: SupportTicket];
}>();

const visible = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value)
});
const loading = ref(false);
const imageFiles = ref<any[]>([]);
const rawImages = ref<File[]>([]);
const form = reactive({
    username: '',
    contactName: '',
    contactValue: '',
    category: 'FUNCTION' as SupportTicket['category'],
    priority: 'NORMAL' as SupportTicket['priority'],
    description: ''
});

const categoryOptions: Array<{ label: string; value: SupportTicket['category'] }> = [
    { label: '账号与登录', value: 'ACCOUNT' },
    { label: '权限访问', value: 'PERMISSION' },
    { label: '功能异常', value: 'FUNCTION' },
    { label: '数据问题', value: 'DATA' },
    { label: '其他问题', value: 'OTHER' }
];
const priorityOptions: Array<{ label: string; value: SupportTicket['priority'] }> = [
    { label: '低', value: 'LOW' },
    { label: '普通', value: 'NORMAL' },
    { label: '高', value: 'HIGH' },
    { label: '紧急', value: 'URGENT' }
];

watch(visible, (open) => {
    if (!open) {
        reset();
    }
});

function reset() {
    form.username = '';
    form.contactName = '';
    form.contactValue = '';
    form.category = 'FUNCTION';
    form.priority = 'NORMAL';
    form.description = '';
    imageFiles.value = [];
    rawImages.value = [];
}

function onImageChange(file: any) {
    const raw = file?.raw as File | undefined;
    if (!raw) {
        return;
    }
    const validTypes = ['image/png', 'image/jpeg', 'image/webp'];
    if (!validTypes.includes(raw.type) || raw.size > 5 * 1024 * 1024) {
        ElMessage.warning('仅支持不超过 5MB 的 PNG、JPG、JPEG 或 WEBP 图片');
        return;
    }
    rawImages.value = [...rawImages.value, raw];
    imageFiles.value = [...imageFiles.value, file];
}

function onImageRemove(file: any) {
    const raw = file?.raw as File | undefined;
    imageFiles.value = imageFiles.value.filter((item) => item.uid !== file.uid);
    if (raw) {
        rawImages.value = rawImages.value.filter((item) => item !== raw);
    }
}

function validate() {
    if (!form.description.trim()) {
        ElMessage.warning('请描述遇到的问题');
        return false;
    }
    if (!props.authenticated && (!form.contactName.trim() || !form.contactValue.trim())) {
        ElMessage.warning('请填写姓名和联系方式');
        return false;
    }
    return true;
}

async function submit() {
    if (!validate() || loading.value) {
        return;
    }
    loading.value = true;
    try {
        const ticket = props.authenticated
            ? await api.createSupportTicket({
                category: form.category,
                priority: form.priority,
                description: form.description.trim(),
                pageContext: props.context || undefined
            })
            : await api.createGuestSupportTicket({
                username: form.username.trim() || undefined,
                contactName: form.contactName.trim(),
                contactValue: form.contactValue.trim(),
                category: form.category,
                priority: form.priority,
                description: form.description.trim()
            });
        if (props.authenticated) {
            for (const image of rawImages.value) {
                await api.uploadSupportTicketImage(ticket.id, image);
            }
        }
        ElMessage.success('支持工单已提交，处理进度会在平台内更新');
        emit('submitted', ticket);
        visible.value = false;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '支持工单提交失败'));
    }
    finally {
        loading.value = false;
    }
}
</script>

<template>
    <!-- 工单提交弹窗 -->
    <el-dialog v-model="visible" class="support-ticket-dialog" :title="authenticated ? '提交支持工单' : '联系技术支持'" width="min(540px, calc(100vw - 32px))" destroy-on-close>
        <el-form label-position="top">
            <template v-if="!authenticated">
                <el-form-item label="账号（选填）">
                    <el-input v-model="form.username" autocomplete="username" placeholder="请输入登录账号" />
                </el-form-item>
                <div class="support-ticket-dialog__pair">
                    <el-form-item label="姓名">
                        <el-input v-model="form.contactName" placeholder="请输入姓名" />
                    </el-form-item>
                    <el-form-item label="联系方式">
                        <el-input v-model="form.contactValue" placeholder="手机或工作邮箱" />
                    </el-form-item>
                </div>
            </template>
            <div class="support-ticket-dialog__pair">
                <el-form-item label="问题分类">
                    <el-select v-model="form.category">
                        <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="紧急程度">
                    <el-select v-model="form.priority">
                        <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </el-form-item>
            </div>
            <el-form-item label="问题描述">
                <el-input v-model="form.description" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="请说明操作步骤、出现的现象及希望获得的协助" />
            </el-form-item>
            <el-form-item v-if="authenticated" label="问题截图">
                <el-upload
                    :auto-upload="false"
                    :file-list="imageFiles"
                    :limit="3"
                    accept="image/png,image/jpeg,image/webp"
                    @change="onImageChange"
                    @remove="onImageRemove"
                >
                    <el-button :icon="'Picture'">添加图片</el-button>
                    <template #tip><div class="el-upload__tip">PNG、JPG、JPEG 或 WEBP，单张不超过 5MB。</div></template>
                </el-upload>
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="primary" :loading="loading" @click="submit">提交工单</el-button>
        </template>
    </el-dialog>
</template>

<style scoped>
/* 工单弹窗局部样式。 */
.support-ticket-dialog__pair {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 16px;
}

.support-ticket-dialog__pair :deep(.el-select) {
    width: 100%;
}

@media (max-width: 560px) {
    .support-ticket-dialog__pair {
        grid-template-columns: 1fr;
        gap: 0;
    }
}
</style>
