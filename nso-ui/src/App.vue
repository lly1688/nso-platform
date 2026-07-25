<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = computed(() => router.getRoutes().filter((item) => item.meta?.title))
const isLogin = computed(() => route.path === '/login')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <RouterView v-if="isLogin" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">NS</div>
        <div>
          <strong>智慧非标协同</strong>
          <span>订单打样与变更闭环</span>
        </div>
      </div>
      <nav class="nav-list">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          <el-icon><component :is="item.meta.icon || 'Menu'" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ route.meta.title || '工作台' }}</h1>
          <p>真实 MVP 闭环：建档、发布、确认、变更、任务阻断和风险复盘</p>
        </div>
        <div class="top-actions">
          <el-tag type="success" effect="plain">MVP 联调</el-tag>
          <el-button :icon="'SwitchButton'" @click="logout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
