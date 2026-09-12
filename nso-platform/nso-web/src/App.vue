<script setup lang="ts">
// 应用壳层的导航与会话展示状态。
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import RoleAvatar from '@/components/RoleAvatar.vue';
import BrandLogo from '@/components/BrandLogo.vue';
import HelpDrawer from '@/components/HelpDrawer.vue';
import { roleTheme } from '@/utils/role-theme';
import { resolveFirstAccessibleRoute } from '@/router';
import zhCn from 'element-plus/es/locale/lang/zh-cn';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const sidebarPreferenceKey = 'nso-web-sidebar-collapsed';
const isSidebarCollapsed = ref(readSidebarPreference());
const searchKeyword = ref('');
const helpOpen = ref(false);
const navItems = computed(() => {
    const items = router.getRoutes().filter((item) => {
        const sidebarPermissions = item.meta?.sidebarPermissions;
        return item.meta?.title
            && item.meta?.sidebar !== false
            && (!item.meta.permission || auth.can(String(item.meta.permission)))
            && (!Array.isArray(sidebarPermissions) || sidebarPermissions.some((permission) => auth.can(String(permission))));
    });
    const taskItems = items.filter((item) => ['tasks-execution', 'tasks-risks', 'tasks-deliveries'].includes(String(item.name)));
    const taskNames = new Set(taskItems.map((item) => item.name));
    const orderedItems = items.filter((item) => !taskNames.has(item.name));
    const changeIndex = orderedItems.findIndex((item) => item.name === 'changes');
    orderedItems.splice(changeIndex >= 0 ? changeIndex + 1 : orderedItems.length, 0, ...taskItems);
    return orderedItems;
});
const isStandalone = computed(() => route.path === '/login' || route.meta.public);
const currentTitle = computed(() => String(route.meta.title || '仪表盘'));
const showCreateProject = computed(() => auth.can('project:create')
    && ['dashboard', 'projects', 'project-workspace'].includes(String(route.name)));
const pageBreadcrumbs = computed(() => {
    const matchedTitles = route.matched
        .map((item) => String(item.meta?.title || ''))
        .filter(Boolean);
    const sourceTitles = matchedTitles.length ? matchedTitles : [currentTitle.value];
    const uniqueTitles = sourceTitles.filter((title, index) => sourceTitles.indexOf(title) === index);
    return ['仪表盘', ...uniqueTitles.filter((title) => title !== '仪表盘')];
});

const userDisplayName = computed(() => auth.profile?.nickname || auth.user?.nickname || auth.user?.username || '用户');
const currentRole = computed(() => roleTheme(auth.profile?.roles || auth.user?.roles));

function readSidebarPreference() {
    try {
        return localStorage.getItem(sidebarPreferenceKey) === 'true';
    }
    catch {
        return false;
    }
}

function toggleSidebar() {
    isSidebarCollapsed.value = !isSidebarCollapsed.value;
}

function createProject() {
    void router.push({ name: 'projects', query: { create: '1' } });
}

function search() {
    const keyword = searchKeyword.value.trim();
    if (!keyword) {
        return;
    }
    const landing = resolveFirstAccessibleRoute(auth.user);
    void router.push({ path: landing, query: { keyword } });
}

function help() {
    helpOpen.value = true;
}

async function logout() {
    await auth.logout();
    await router.push({ name: 'login' });
}

function openProfile() {
    void router.push({ name: 'profile' });
}

function syncSession(event: Event) {
    const session = (event as CustomEvent).detail;
    if (session?.accessToken) {
        auth.saveSession(session);
    }
}

onMounted(() => {
    window.addEventListener('nso-session-refreshed', syncSession);
    if (auth.token) {
        void auth.loadProfile().catch(() => undefined);
    }
});

onUnmounted(() => window.removeEventListener('nso-session-refreshed', syncSession));

watch(isSidebarCollapsed, (collapsed) => {
    try {
        localStorage.setItem(sidebarPreferenceKey, String(collapsed));
    }
    catch {
        // 存储不可用时仍保留当前会话的导航状态。
    }
});
</script>

<template>
    <!-- 应用导航与主内容 -->
    <el-config-provider :locale="zhCn">
    <RouterView v-if="isStandalone" />
    <div v-else class="app-shell" :class="{ 'app-shell--collapsed': isSidebarCollapsed }">
        <a class="skip-link" href="#main-content">跳转至主要内容</a>
        <aside class="sidebar" :aria-label="isSidebarCollapsed ? '主导航，已收起' : '主导航'">
            <BrandLogo class="brand" :compact="isSidebarCollapsed" />

            <el-button
                v-if="showCreateProject"
                class="create-project"
                type="primary"
                :icon="'Plus'"
                :aria-label="isSidebarCollapsed ? '新建项目' : undefined"
                @click="createProject"
            ><span v-if="!isSidebarCollapsed">新建项目</span></el-button>

            <nav class="nav-list">
                <div v-for="item in navItems" :key="item.path" class="nav-entry">
                    <div v-if="item.name === 'tasks-execution' && !isSidebarCollapsed" class="nav-section-label">任务中心</div>
                    <RouterLink :to="item.path" class="nav-item" :title="isSidebarCollapsed ? String(item.meta.title) : undefined">
                    <el-icon><component :is="item.meta.icon || 'Menu'" /></el-icon>
                    <span v-show="!isSidebarCollapsed">{{ item.meta.title }}</span>
                    </RouterLink>
                </div>
            </nav>

            <div class="sidebar-footer">
                <RouterLink v-if="auth.can('sys:user:read')" to="/system" class="nav-item">
                    <el-icon><Setting /></el-icon>
                    <span v-show="!isSidebarCollapsed">设置</span>
                </RouterLink>
                <button type="button" class="nav-item nav-item--button" @click="help">
                    <el-icon><QuestionFilled /></el-icon>
                    <span v-show="!isSidebarCollapsed">帮助</span>
                </button>
                <button
                    type="button"
                    class="rail-toggle"
                    :aria-label="isSidebarCollapsed ? '展开导航' : '收起导航'"
                    :aria-pressed="isSidebarCollapsed"
                    @click="toggleSidebar"
                >
                    <el-icon><Expand v-if="isSidebarCollapsed" /><Fold v-else /></el-icon>
                    <span v-if="!isSidebarCollapsed">收起导航</span>
                </button>
            </div>
        </aside>

        <section class="workspace">
            <header class="topbar">
                <div class="topbar-leading">
                    <div class="topbar-context" aria-live="polite">
                        <span>{{ pageBreadcrumbs.slice(0, -1).join(' / ') || '智慧非标协同平台' }}</span>
                        <strong>{{ currentTitle }}</strong>
                    </div>
                    <el-input v-model="searchKeyword" class="global-search" placeholder="搜索项目、图纸..." :prefix-icon="'Search'" clearable @keyup.enter="search" />
                </div>
                <div class="top-actions">
                    <el-button text :icon="'Bell'" aria-label="通知" @click="router.push(resolveFirstAccessibleRoute(auth.user))" />
                    <el-button v-if="auth.can('sys:user:read')" text :icon="'Setting'" aria-label="系统设置" @click="router.push({ name: 'system' })" />
                    <el-dropdown trigger="click">
                        <button type="button" class="user-menu" aria-label="用户菜单">
                            <RoleAvatar :roles="auth.profile?.roles || auth.user?.roles" :src="auth.avatarObjectUrl" :size="36" />
                            <span class="user-menu__text">
                                <span class="user-name">{{ userDisplayName }}</span>
                                <span class="user-role">{{ currentRole.label }}</span>
                            </span>
                        </button>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item :icon="'User'" @click="openProfile">个人中心</el-dropdown-item>
                                <el-dropdown-item divided :icon="'SwitchButton'" @click="logout">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </header>
            <main id="main-content" class="content" tabindex="-1">
                <RouterView />
            </main>
        </section>
    </div>
    <HelpDrawer v-if="!isStandalone" v-model="helpOpen" />
    </el-config-provider>
</template>
