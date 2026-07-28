<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { userFacingError } from '@/utils/request'
import { canAccessPath, resolveFirstAccessibleRoute } from '@/router'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const errorText = ref('')
const form = reactive({ username: 'admin', password: 'admin123' })
const demoAccounts = import.meta.env.DEV ? [
  { username: 'demo-admin', label: '系统管理员' },
  { username: 'demo-pm', label: '项目经理' },
  { username: 'demo-tech', label: '技术设计' },
  { username: 'demo-process', label: '工艺工程' },
  { username: 'demo-purchase', label: '采购供应' },
  { username: 'demo-production', label: '计划生产' },
  { username: 'demo-quality', label: '质量管理' },
  { username: 'demo-customer', label: '客户确认' },
  { username: 'demo-executive', label: '经营管理' }
] : []

async function submit() {
  errorText.value = ''
  if (!form.username.trim() || !form.password) {
    errorText.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : resolveFirstAccessibleRoute(auth.user)
    await router.replace(canAccessPath(redirect, auth.user) ? redirect : resolveFirstAccessibleRoute(auth.user))
  } catch (error) {
    errorText.value = userFacingError(error, '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}

function selectDemo(username: string) {
  form.username = username
  form.password = 'demo-password-123'
}

function support() {
  ElMessage.info('请联系技术支持获取账号协助')
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-brand">
        <div class="login-brand__mark"><Connection /></div>
        <div>
          <h1 id="login-title">智慧非标协同平台</h1>
          <p><el-icon><InfoFilled /></el-icon> 演示账号：admin / admin123</p>
        </div>
      </div>

      <el-alert v-if="errorText" class="login-error" type="error" :title="errorText" show-icon :closable="false" />

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item v-if="demoAccounts.length" label="开发演示账号">
          <el-select placeholder="选择账号后自动填充" @change="selectDemo">
            <el-option v-for="account in demoAccounts" :key="account.username" :label="`${account.label} · ${account.username}`" :value="account.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" :prefix-icon="'User'" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" :prefix-icon="'Lock'" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button class="login-submit" native-type="submit" type="primary" :loading="loading">登录</el-button>
      </el-form>

      <footer class="login-footer">
        <button type="button" @click="support">忘记密码？</button>
        <button type="button" @click="support">联系技术支持</button>
      </footer>
    </section>
  </main>
</template>
