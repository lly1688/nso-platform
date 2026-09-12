<script setup lang="ts">
// 登录与密码恢复表单状态。
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import BrandLogo from '@/components/BrandLogo.vue';
import SupportTicketDialog from '@/components/SupportTicketDialog.vue';
import { api } from '@/api';
import { userFacingError } from '@/utils/request';
import { canAccessPath, resolveFirstAccessibleRoute } from '@/router';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const loading = ref(false);
const errorText = ref('');
const form = reactive({ username: '', password: '' });
const recoveryOpen = ref(false);
const supportOpen = ref(false);
const recoveryLoading = ref(false);
const recoveryForm = reactive({ username: '', contactName: '', contactValue: '', requesterNote: '' });

async function submit() {
    if (loading.value) {
        return;
    }
    errorText.value = '';
    if (!form.username.trim() || !form.password) {
        errorText.value = '请输入账号和密码';
        return;
    }
    loading.value = true;
    try {
        await auth.login(form.username.trim(), form.password);
        if (auth.user?.forceChangePassword) {
            await router.replace({ name: 'profile' });
            return;
        }
        const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
            ? route.query.redirect
            : resolveFirstAccessibleRoute(auth.user);
        await router.replace(canAccessPath(redirect, auth.user) ? redirect : resolveFirstAccessibleRoute(auth.user));
    }
    catch (error) {
        errorText.value = userFacingError(error, '登录失败，请检查账号和密码');
    }
    finally {
        loading.value = false;
    }
}

async function submitRecovery() {
    if (recoveryLoading.value) {
        return;
    }
    if (!recoveryForm.username.trim() || !recoveryForm.contactName.trim() || !recoveryForm.contactValue.trim()) {
        ElMessage.warning('请填写账号、姓名和联系方式');
        return;
    }
    recoveryLoading.value = true;
    try {
        await api.requestPasswordRecovery({
            username: recoveryForm.username.trim(),
            contactName: recoveryForm.contactName.trim(),
            contactValue: recoveryForm.contactValue.trim(),
            requesterNote: recoveryForm.requesterNote.trim() || undefined
        });
        ElMessage.success('申请已提交；如信息核验通过，技术支持会通过登记联系方式协助重置密码');
        recoveryOpen.value = false;
        recoveryForm.username = '';
        recoveryForm.contactName = '';
        recoveryForm.contactValue = '';
        recoveryForm.requesterNote = '';
    }
    catch (error) {
        ElMessage.error(userFacingError(error, '密码恢复申请提交失败'));
    }
    finally {
        recoveryLoading.value = false;
    }
}
</script>

<template>
    <!-- 登录表单与密码恢复入口 -->
    <main class="login-page">
        <section class="login-hero" aria-labelledby="login-purpose-title">
            <div class="login-hero__content">
                <header class="login-hero__header">
                    <BrandLogo class="login-hero__brand" size="lg" label="智慧非标协同平台" tagline="" />
                </header>

                <div class="login-hero__copy">
                    <h1 id="login-purpose-title">让每张非标订单<br><span>清晰走到交付</span></h1>
                    <p class="login-hero__description">统一客户需求、技术版本、样品确认和变更任务，让销售、技术、项目与生产在同一工作台协同推进。</p>
                </div>

                <section class="login-purpose" aria-label="平台协同能力">
                    <h2>从需求到交付，在一处协同</h2>
                    <ul class="login-purpose__list">
                        <li>
                            <strong>需求与报价</strong>
                            <span>归集客户要求、交期和技术条件</span>
                        </li>
                        <li>
                            <strong>技术与打样</strong>
                            <span>沉淀图纸、样品和确认记录</span>
                        </li>
                        <li>
                            <strong>变更与交付</strong>
                            <span>同步影响范围、执行任务和交付状态</span>
                        </li>
                    </ul>
                </section>
            </div>
        </section>

        <section class="login-credential" aria-labelledby="login-title">
            <section class="login-panel">
                <div class="login-brand">
                    <h1 id="login-title">登录工作台</h1>
                    <p>继续处理订单、打样与变更协同工作</p>
                </div>

                <el-alert v-if="errorText" class="login-error" type="error" :title="errorText" show-icon :closable="false" />

                <el-form class="login-form" label-position="top" @submit.prevent="submit">
                    <el-form-item label="账号">
                        <el-input v-model="form.username" autocomplete="username" placeholder="请输入账号" :prefix-icon="'User'" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input
                            v-model="form.password"
                            type="password"
                            autocomplete="current-password"
                            placeholder="请输入密码"
                            :prefix-icon="'Lock'"
                            show-password
                        />
                    </el-form-item>
                    <el-button class="login-submit" native-type="submit" type="primary" :loading="loading">登录</el-button>
                </el-form>

                <footer class="login-footer">
                    <button type="button" @click="recoveryOpen = true">忘记密码？</button>
                    <button type="button" @click="supportOpen = true">联系技术支持</button>
                </footer>
            </section>
        </section>
    </main>
    <!-- 操作弹窗 -->
    <el-dialog v-model="recoveryOpen" title="申请密码恢复" width="min(500px, calc(100vw - 32px))" destroy-on-close>
        <p class="login-recovery-intro">提交后系统管理员会通过线下方式核验身份并协助重置。为保护账号安全，系统不会显示账号是否存在。</p>
        <el-form label-position="top" @submit.prevent="submitRecovery">
            <el-form-item label="账号">
                <el-input v-model="recoveryForm.username" autocomplete="username" placeholder="请输入登录账号" />
            </el-form-item>
            <div class="login-recovery-pair">
                <el-form-item label="姓名">
                    <el-input v-model="recoveryForm.contactName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="联系方式">
                    <el-input v-model="recoveryForm.contactValue" placeholder="手机或工作邮箱" />
                </el-form-item>
            </div>
            <el-form-item label="补充说明（选填）">
                <el-input v-model="recoveryForm.requesterNote" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="可补充身份核验所需信息" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="recoveryOpen = false">取消</el-button>
            <el-button type="primary" :loading="recoveryLoading" @click="submitRecovery">提交申请</el-button>
        </template>
    </el-dialog>
    <SupportTicketDialog v-model="supportOpen" />
</template>
