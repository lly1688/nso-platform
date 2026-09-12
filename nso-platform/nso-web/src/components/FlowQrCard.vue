<script setup lang="ts">
// 流程二维码展示数据。
import { onMounted, ref, watch } from 'vue';
import QRCode from 'qrcode';
import { ElMessage } from 'element-plus';

const props = withDefaults(defineProps<{
    code: string;
    payload: string;
    title?: string;
    subtitle?: string;
    compact?: boolean;
}>(), {
    title: '现场快速流转',
    subtitle: ''
});

const dataUrl = ref('');
const generating = ref(false);

async function renderCode() {
    if (!props.payload) {
        return;
    }
    generating.value = true;
    try {
        dataUrl.value = await QRCode.toDataURL(props.payload, {
            width: props.compact ? 132 : 184,
            margin: 1,
            errorCorrectionLevel: 'M',
            color: { dark: '#111827', light: '#f8fafc' }
        });
    }
    catch {
        ElMessage.error('二维码生成失败，请刷新后重试');
    }
    finally {
        generating.value = false;
    }
}

function printCode() {
    window.print();
}

watch(
    () => props.payload,
    () => {
        void renderCode();
    }
);
onMounted(() => {
    void renderCode();
});
</script>

<template>
    <!-- 二维码信息卡 -->
    <section class="flow-qr-card" :class="{ 'flow-qr-card--compact': compact }">
        <div class="flow-qr-card__heading">
            <div>
                <h3>{{ title }}</h3>
                <p v-if="subtitle">{{ subtitle }}</p>
            </div>
            <el-button text :icon="'Printer'" aria-label="打印二维码" @click="printCode" />
        </div>
        <div class="flow-qr-card__body" v-loading="generating">
            <img v-if="dataUrl" class="flow-qr-card__image" :src="dataUrl" alt="项目流转二维码" />
        </div>
        <p class="flow-qr-card__code">{{ code }}</p>
        <el-button class="flow-qr-card__print" text type="primary" :icon="'Printer'" @click="printCode">打印物料流转卡</el-button>
    </section>
</template>
