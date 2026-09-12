<script setup lang="ts">
// 帮助检索与抽屉状态。
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute } from 'vue-router';
import { api } from '@/api';
import type { SupportTicket, SupportTicketAttachment } from '@/types';
import { searchTopics, topicForRoute } from '@/help/catalog';
import { userFacingError } from '@/utils/request';
import SupportTicketDialog from './SupportTicketDialog.vue';

const props = defineProps<{
    modelValue: boolean;
}>();
const emit = defineEmits<{
    'update:modelValue': [value: boolean];
}>();

const route = useRoute();
const activeTab = ref<'guide' | 'tickets'>('guide');
const keyword = ref('');
const supportDialogOpen = ref(false);
const tickets = ref<SupportTicket[]>([]);
const ticketsLoading = ref(false);

const visible = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value)
});
const currentTopic = computed(() => topicForRoute(route.name));
const matchingTopics = computed(() => searchTopics(keyword.value));
const context = computed(() => `${String(route.meta.title || currentTopic.value.title)} · ${route.fullPath}`);

watch([visible, activeTab], ([open, tab]) => {
    if (open && tab === 'tickets') {
        void loadTickets();
    }
});

async function loadTickets() {
    ticketsLoading.value = true;
    try {
        tickets.value = (await api.mySupportTickets()).list;
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '工单列表加载失败'));
    }
    finally {
        ticketsLoading.value = false;
    }
}

async function openAttachment(ticket: SupportTicket, attachment: SupportTicketAttachment) {
    try {
        const response = await api.downloadSupportTicketImage(ticket.id, attachment.id);
        const url = URL.createObjectURL(response);
        window.open(url, '_blank', 'noopener');
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '图片打开失败'));
    }
}
</script>

<template>
    <!-- 帮助主题与检索结果 -->
    <el-drawer v-model="visible" class="help-drawer" direction="rtl" size="min(440px, 100vw)" :with-header="false">
        <header class="help-drawer__header">
            <div>
                <p class="help-drawer__eyebrow">当前页面</p>
                <h2>{{ currentTopic.title }}</h2>
                <p>{{ currentTopic.summary }}</p>
            </div>
            <el-button text :icon="'Close'" aria-label="关闭帮助" @click="visible = false" />
        </header>

        <el-tabs v-model="activeTab" class="help-drawer__tabs" stretch>
            <el-tab-pane label="使用指南" name="guide">
                <el-input v-model="keyword" clearable :prefix-icon="'Search'" placeholder="搜索帮助主题或问题" />
                <section class="help-drawer__topic">
                    <h3>{{ currentTopic.title }}</h3>
                    <ol>
                        <li v-for="step in currentTopic.steps" :key="step">{{ step }}</li>
                    </ol>
                    <el-alert v-if="currentTopic.roleTip" :title="currentTopic.roleTip" type="info" :closable="false" show-icon />
                </section>
                <section class="help-drawer__topic">
                    <h3>常见问题</h3>
                    <el-collapse>
                        <el-collapse-item v-for="faq in currentTopic.faqs" :key="faq.question" :title="faq.question">
                            <p>{{ faq.answer }}</p>
                        </el-collapse-item>
                    </el-collapse>
                </section>
                <section v-if="keyword" class="help-drawer__topic">
                    <h3>匹配主题</h3>
                    <div class="help-drawer__matches">
                        <button v-for="topic in matchingTopics" :key="topic.id" type="button" @click="keyword = topic.title">
                            <strong>{{ topic.title }}</strong>
                            <span>{{ topic.summary }}</span>
                        </button>
                    </div>
                </section>
                <footer class="help-drawer__actions">
                    <el-button :icon="'Memo'" @click="activeTab = 'tickets'">我的工单</el-button>
                    <el-button type="primary" :icon="'Service'" @click="supportDialogOpen = true">提交支持工单</el-button>
                </footer>
            </el-tab-pane>
            <el-tab-pane label="我的工单" name="tickets">
                <div class="help-drawer__ticket-actions">
                    <span>处理进度会在这里更新</span>
                    <el-button text :icon="'Refresh'" aria-label="刷新工单" @click="loadTickets" />
                </div>
                <div v-loading="ticketsLoading" class="help-drawer__ticket-list">
                    <article v-for="ticket in tickets" :key="ticket.id" class="help-drawer__ticket">
                        <div class="help-drawer__ticket-title">
                            <strong>{{ ticket.ticketNo }}</strong>
                            <el-tag size="small" :type="ticket.status === 'RESOLVED' || ticket.status === 'CLOSED' ? 'success' : ticket.priority === 'URGENT' ? 'danger' : 'warning'">{{ ticket.status }}</el-tag>
                        </div>
                        <p>{{ ticket.description }}</p>
                        <small v-if="ticket.handlingNote">处理说明：{{ ticket.handlingNote }}</small>
                        <div v-if="ticket.attachments.length" class="help-drawer__attachments">
                            <button v-for="attachment in ticket.attachments" :key="attachment.id" type="button" @click="openAttachment(ticket, attachment)">
                                <el-icon><Picture /></el-icon>{{ attachment.fileName }}
                            </button>
                        </div>
                    </article>
                    <el-empty v-if="!ticketsLoading && !tickets.length" description="暂无支持工单" :image-size="72" />
                </div>
                <footer class="help-drawer__actions">
                    <el-button type="primary" :icon="'Service'" @click="supportDialogOpen = true">提交支持工单</el-button>
                </footer>
            </el-tab-pane>
        </el-tabs>
    </el-drawer>
    <SupportTicketDialog v-model="supportDialogOpen" authenticated :context="context" @submitted="loadTickets" />
</template>

<style scoped>
/* 帮助抽屉局部样式。 */
.help-drawer__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    padding: 4px 4px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.help-drawer__header h2,
.help-drawer__header p,
.help-drawer__topic h3,
.help-drawer__topic p {
    margin: 0;
}

.help-drawer__header h2 {
    color: var(--nso-ink);
    font-size: 20px;
    line-height: 1.35;
}

.help-drawer__header > div > p:last-child {
    margin-top: 7px;
    color: var(--nso-muted);
    font-size: 13px;
    line-height: 1.6;
}

.help-drawer__eyebrow {
    margin-bottom: 5px !important;
    color: var(--nso-blue-dark);
    font-size: 12px;
    font-weight: 600;
}

.help-drawer__tabs :deep(.el-tabs__content) {
    padding-top: 18px;
}

.help-drawer__topic {
    padding: 20px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.help-drawer__topic h3 {
    margin-bottom: 12px;
    color: var(--nso-ink);
    font-size: 14px;
}

.help-drawer__topic ol {
    display: grid;
    gap: 10px;
    margin: 0;
    padding-left: 22px;
    color: var(--nso-ink-soft);
    font-size: 13px;
    line-height: 1.65;
}

.help-drawer__topic :deep(.el-alert) {
    margin-top: 16px;
}

.help-drawer__matches,
.help-drawer__ticket-list {
    display: grid;
    gap: 10px;
}

.help-drawer__matches button,
.help-drawer__attachments button {
    display: grid;
    gap: 3px;
    width: 100%;
    padding: 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: var(--nso-radius);
    background: #fff;
    color: inherit;
    text-align: left;
    cursor: pointer;
}

.help-drawer__matches button:hover,
.help-drawer__attachments button:hover {
    border-color: #cfdbff;
    background: var(--nso-blue-soft);
}

.help-drawer__matches span {
    color: var(--nso-muted);
    font-size: 12px;
    line-height: 1.5;
}

.help-drawer__actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 20px 0 4px;
}

.help-drawer__ticket-actions,
.help-drawer__ticket-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.help-drawer__ticket-actions {
    margin-bottom: 14px;
    color: var(--nso-muted);
    font-size: 12px;
}

.help-drawer__ticket {
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: var(--nso-radius);
    background: #fff;
}

.help-drawer__ticket p {
    margin: 10px 0 6px;
    color: var(--nso-ink-soft);
    font-size: 13px;
    line-height: 1.55;
}

.help-drawer__ticket small {
    display: block;
    color: var(--nso-muted);
    font-size: 12px;
    line-height: 1.5;
}

.help-drawer__attachments {
    display: grid;
    gap: 6px;
    margin-top: 10px;
}

.help-drawer__attachments button {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--nso-blue-dark);
    font-size: 12px;
}
</style>
