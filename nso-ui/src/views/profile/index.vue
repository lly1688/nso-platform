<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import RoleAvatar from '@/components/RoleAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { roleTheme } from '@/utils/role-theme'
import { userFacingError } from '@/utils/request'
import type { UserProfile } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const fileInput = ref<HTMLInputElement>()
const saving = ref(false)
const changingPassword = ref(false)
const profileForm = reactive<Pick<UserProfile, 'nickname' | 'phone' | 'email' | 'gender' | 'version'>>({
  nickname: '', phone: '', email: '', gender: 'UNSPECIFIED', version: 0
})
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const profile = computed(() => auth.profile)
const theme = computed(() => roleTheme(profile.value?.roles || auth.user?.roles))

function setForm() {
  if (!profile.value) return
  profileForm.nickname = profile.value.nickname || ''
  profileForm.phone = profile.value.phone || ''
  profileForm.email = profile.value.email || ''
  profileForm.gender = profile.value.gender || 'UNSPECIFIED'
  profileForm.version = profile.value.version
}

async function load() {
  try {
    await auth.loadProfile()
    setForm()
  } catch (error) {
    ElMessage.error(userFacingError(error, '个人资料加载失败'))
  }
}

async function saveProfile() {
  saving.value = true
  try {
    const updated = await api.updateProfile({ ...profileForm })
    auth.saveProfile(updated)
    setForm()
    ElMessage.success('个人资料已保存')
  } catch (error) {
    ElMessage.error(userFacingError(error, '个人资料保存失败'))
  } finally {
    saving.value = false
  }
}

function chooseAvatar() {
  fileInput.value?.click()
}

async function uploadAvatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type) || file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像仅支持 JPEG、PNG、WebP，且不能超过 2 MB')
    ;(event.target as HTMLInputElement).value = ''
    return
  }
  try {
    const updated = await api.uploadAvatar(file)
    auth.saveProfile(updated)
    await auth.loadProfile()
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(userFacingError(error, '头像上传失败'))
  } finally {
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function savePassword() {
  if (passwordForm.newPassword.length < 8) {
    ElMessage.error('新密码至少需要 8 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await api.changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    await auth.logout()
    ElMessage.success('密码已修改，请使用新密码重新登录')
    await router.replace({ name: 'login' })
  } catch (error) {
    ElMessage.error(userFacingError(error, '密码修改失败'))
  } finally {
    changingPassword.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="profile-page">
    <header class="profile-heading">
      <div>
        <p class="eyebrow">账户设置</p>
        <h1>个人中心</h1>
        <p>维护本人资料、头像与登录密码。</p>
      </div>
    </header>

    <div class="profile-layout">
      <section class="panel profile-identity">
        <div class="profile-identity__avatar">
          <RoleAvatar :roles="profile?.roles || auth.user?.roles" :src="auth.avatarObjectUrl" :size="96" />
          <button type="button" class="avatar-edit" @click="chooseAvatar"><el-icon><Camera /></el-icon>更换头像</button>
          <input ref="fileInput" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" @change="uploadAvatar" />
        </div>
        <div class="profile-identity__name">
          <h2>{{ profile?.nickname || auth.user?.nickname || auth.user?.username }}</h2>
          <el-tag effect="plain">{{ theme.label }}</el-tag>
        </div>
        <dl class="profile-summary">
          <div><dt>账号</dt><dd>{{ profile?.username || auth.user?.username }}</dd></div>
          <div><dt>所属部门</dt><dd>{{ profile?.departmentName || '未分配部门' }}</dd></div>
          <div><dt>所属角色</dt><dd>{{ (profile?.roles || auth.user?.roles || []).map((role: string) => roleTheme([role]).label).join('、') }}</dd></div>
          <div><dt>创建时间</dt><dd>{{ profile?.createdAt || '-' }}</dd></div>
        </dl>
      </section>

      <div class="profile-forms">
        <section class="panel">
          <div class="panel-header"><span class="panel-title">基本资料</span></div>
          <div class="panel-body">
            <el-form class="profile-form" label-position="top" @submit.prevent="saveProfile">
              <el-form-item label="用户昵称" required><el-input v-model="profileForm.nickname" maxlength="64" show-word-limit /></el-form-item>
              <div class="profile-form__grid">
                <el-form-item label="手机号码"><el-input v-model="profileForm.phone" maxlength="32" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="profileForm.email" maxlength="128" /></el-form-item>
              </div>
              <el-form-item label="性别">
                <el-radio-group v-model="profileForm.gender">
                  <el-radio value="UNSPECIFIED">未设置</el-radio>
                  <el-radio value="MALE">男</el-radio>
                  <el-radio value="FEMALE">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-button native-type="submit" type="primary" :loading="saving">保存资料</el-button>
            </el-form>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header"><span class="panel-title">修改密码</span></div>
          <div class="panel-body">
            <el-form class="profile-form profile-password" label-position="top" @submit.prevent="savePassword">
              <el-form-item label="当前密码" required><el-input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" show-password /></el-form-item>
              <div class="profile-form__grid">
                <el-form-item label="新密码" required><el-input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" show-password /></el-form-item>
                <el-form-item label="确认新密码" required><el-input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" show-password /></el-form-item>
              </div>
              <el-button native-type="submit" type="primary" plain :loading="changingPassword">修改密码并重新登录</el-button>
            </el-form>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
