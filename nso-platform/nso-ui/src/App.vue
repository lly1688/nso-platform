<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import RoleAvatar from '@/components/RoleAvatar.vue'
import { roleTheme } from '@/utils/role-theme'
import { resolveFirstAccessibleRoute } from '@/router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isSidebarCollapsed = ref(false)
const searchKeyword = ref('')

const navItems = computed(() => router.getRoutes().filter((item) =>
  item.meta?.title && item.meta?.sidebar !== false && (!item.meta.permission || auth.can(String(item.meta.permission)))
))
const isStandalone = computed(() => route.path === '/login' || route.meta.public)
const currentTitle = computed(() => String(route.meta.title || '仪表盘'))
const userDisplayName = computed(() => auth.profile?.nickname || auth.user?.nickname || auth.user?.username || '用户')
const currentRole = computed(() => roleTheme(auth.profile?.roles || auth.user?.roles))

function createProject() {
  void router.push({ name: 'projects', query: { create: '1' } })
}

function search() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  const landing = resolveFirstAccessibleRoute(auth.user)
  void router.push({ path: landing, query: { keyword } })
}

function help() {
  ElMessage.info('请联系系统技术支持获取协助')
}

async function logout() {
  await auth.logout()
  await router.push({ name: 'login' })
}

function openProfile() {
  void router.push({ name: 'profile' })
}

onMounted(() => {
  if (auth.token) {
    void auth.loadProfile().catch(() => undefined)
  }
})
</script>

<template>
  <RouterView v-if="isStandalone" />
  <div v-else class="app-shell" :class="{ 'app-shell--collapsed': isSidebarCollapsed }">
    <a class="skip-link" href="#main-content">跳转至主要内容</a>
    <aside class="sidebar" :aria-label="isSidebarCollapsed ? '主导航，已收起' : '主导航'">
      <div class="brand">
        <div class="brand-mark">OS</div>
        <div v-show="!isSidebarCollapsed" class="brand-copy">
          <strong>智慧非标协同</strong>
          <span>Industrial OS</span>
        </div>
      </div>

      <el-button v-if="!isSidebarCollapsed && auth.can('project:create')" class="create-project" type="primary" :icon="'Plus'" @click="createProject">新建项目</el-button>

      <nav class="nav-list">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          <el-icon><component :is="item.meta.icon || 'Menu'" /></el-icon>
          <span v-show="!isSidebarCollapsed">{{ item.meta.title }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <RouterLink v-if="auth.can('system:manage')" to="/system" class="nav-item">
          <el-icon><Setting /></el-icon>
          <span v-show="!isSidebarCollapsed">设置</span>
        </RouterLink>
        <button type="button" class="nav-item nav-item--button" @click="help">
          <el-icon><QuestionFilled /></el-icon>
          <span v-show="!isSidebarCollapsed">帮助</span>
        </button>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <el-button
          class="sidebar-toggle"
          text
          :icon="isSidebarCollapsed ? 'Expand' : 'Fold'"
          :aria-label="isSidebarCollapsed ? '展开导航' : '收起导航'"
          @click="isSidebarCollapsed = !isSidebarCollapsed"
        />
        <el-input v-model="searchKeyword" class="global-search" placeholder="搜索项目、图纸..." :prefix-icon="'Search'" clearable @keyup.enter="search" />
        <div class="top-actions">
          <el-button text :icon="'Bell'" aria-label="通知" @click="router.push(resolveFirstAccessibleRoute(auth.user))" />
          <el-button v-if="auth.can('system:manage')" text :icon="'Setting'" aria-label="系统设置" @click="router.push({ name: 'system' })" />
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
        <div v-if="route.name !== 'dashboard'" class="page-context">仪表盘 <span>/</span> {{ currentTitle }}</div>
        <RouterView />
      </main>
    </section>
  </div>
</template>
