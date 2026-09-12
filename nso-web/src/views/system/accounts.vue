<script setup lang="ts">
// 用户账号、角色与交接维护状态。
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import { useAuthStore } from '@/stores/auth';
import type { SystemDept, SystemRole, SystemUser } from '@/types';
import { emptyPage, isEnabledStatus, optional, pageList } from './shared';

const loading = ref(false);
const users = ref<SystemUser[]>([]);
const roles = ref<SystemRole[]>([]);
const departments = ref<SystemDept[]>([]);
const posts = ref<Array<Record<string, unknown>>>([]);
const originalRoles = reactive<Record<number, string[]>>({});
const handoverHints = reactive<Record<number, string>>({});
const lifecycleVisible = ref(false);
const lifecycleTarget = ref<SystemUser | null>(null);
const lifecycleStatus = ref<'DISABLED' | 'LEFT'>('DISABLED');
const lifecycleOutstanding = ref<string[]>([]);
const auth = useAuthStore();
const userForm = reactive({
    username: '',
    password: '',
    nickname: '',
    employeeNo: '',
    deptId: undefined as number | undefined,
    roleCodes: [] as string[],
    postIds: [] as number[]
});
const userPagination = usePagination<SystemUser>();
userPagination.configure(
    (params) => api.systemUsers(params),
    (result) => {
        users.value = result.list;
        result.list.forEach((user) => {
            originalRoles[user.id] = [...(user.roles || [])];
        });
    }
);

async function load() {
    loading.value = true;
    try {
        const [, roleData, deptData, postData] = await Promise.all([
            userPagination.reload(),
            optional(api.systemRoles({ pageSize: 100 }), emptyPage<SystemRole>()),
            optional(api.departments({ pageSize: 100 }), emptyPage<SystemDept>()),
            optional(api.systemPosts({ pageSize: 100 }), emptyPage<Record<string, unknown>>())
        ]);
        roles.value = pageList(roleData);
        departments.value = pageList(deptData);
        posts.value = pageList(postData);
    }
    finally {
        loading.value = false;
    }
}

function resetUserForm() {
    userForm.username = '';
    userForm.password = '';
    userForm.nickname = '';
    userForm.employeeNo = '';
    userForm.deptId = undefined;
    userForm.roleCodes = [];
    userForm.postIds = [];
}

async function createUser() {
    if (!userForm.username.trim() || userForm.password.length < 8) {
        return ElMessage.warning('请填写用户名和至少 8 位的初始密码');
    }
    const created = await api.createSystemUser({ ...userForm }) as {
        id?: number;
    };
    if (created.id && userForm.postIds.length) {
        await api.replaceSystemUserPosts(created.id, userForm.postIds);
    }
    resetUserForm();
    ElMessage.success('人员档案已创建，账号处于待激活状态');
    await load();
}

async function saveUser(row: SystemUser) {
    try {
        await api.updateSystemUser(row.id, {
            nickname: row.nickname,
            deptId: row.deptId,
            status: row.status
        });
        const before = originalRoles[row.id] || [];
        const after = [...(row.roles || [])].sort();
        if (auth.can('sys:role:grant') && JSON.stringify(before.slice().sort()) !== JSON.stringify(after)) {
            await api.replaceSystemUserRoles(row.id, row.roles || []);
        }
        await api.replaceSystemUserPosts(row.id, row.postIds || []);
        ElMessage.success('账号资料已保存；角色或状态变更会要求重新登录');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '账号资料保存失败');
    }
}

async function activateUser(row: SystemUser) {
    try {
        const { value } = await ElMessageBox.prompt(`为 ${row.username} 设置至少 8 位临时密码`, '激活内部账号', {
            inputType: 'password',
            inputPattern: /^.{8,}$/,
            inputErrorMessage: '密码至少 8 位'
        });
        await api.activateSystemUser(row.id, { password: value, reason: '系统管理员审核并激活' });
        ElMessage.success('账号已激活，用户首次登录必须修改密码');
        await load();
    }
    catch {
        // 放弃激活时不改变账号状态。
    }
}

async function checkHandover(row: SystemUser) {
    try {
        const result = await api.systemUserHandoverCheck(row.id);
        const outstanding = Object.entries(result)
            .filter(([, count]) => count > 0)
            .map(([name, count]) => `${name}: ${count}`);
        handoverHints[row.id] = outstanding.length ? `待处理：${outstanding.join('，')}` : '交接校验已通过';
        ElMessage.info(outstanding.length ? `仍需交接：${outstanding.join('，')}` : '交接校验已通过，可执行停用或离职');
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '交接校验失败');
    }
}

async function completeLifecycle(row: SystemUser) {
    try {
        const result = await api.systemUserHandoverCheck(row.id);
        lifecycleOutstanding.value = Object.entries(result)
            .filter(([, count]) => count > 0)
            .map(([name, count]) => `${name}: ${count}`);
        handoverHints[row.id] = lifecycleOutstanding.value.length
            ? `待处理：${lifecycleOutstanding.value.join('，')}`
            : '交接校验已通过';
        lifecycleTarget.value = row;
        lifecycleStatus.value = 'DISABLED';
        lifecycleVisible.value = true;
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '交接校验失败');
    }
}

async function confirmLifecycle() {
    if (!lifecycleTarget.value || lifecycleOutstanding.value.length) return;
    try {
        await api.updateSystemUserLifecycle(lifecycleTarget.value.id, { status: lifecycleStatus.value, reason: 'PC 端交接完成确认' });
        lifecycleVisible.value = false;
        ElMessage.success(lifecycleStatus.value === 'LEFT' ? '账号已标记为离职，旧会话已失效' : '账号已停用，旧会话已失效');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '账号生命周期更新失败');
    }
}

async function resetUserPassword(row: SystemUser) {
    try {
        const { value } = await ElMessageBox.prompt(`为账号 ${row.username} 设置至少 8 位的新密码`, '重置密码', {
            inputType: 'password',
            inputPattern: /^.{8,}$/,
            inputErrorMessage: '密码至少 8 位'
        });
        await api.resetSystemUserPassword(row.id, value);
        ElMessage.success('密码已重置，原会话将失效');
    }
    catch {
        // 密码输入框关闭无需额外提示。
    }
}

function statusLabel(status: string) {
    return ({ ACTIVE: '已激活', PENDING: '待激活', DISABLED: '已停用', LEFT: '已离职', LOCKED: '已锁定', INACTIVE: '未启用' } as Record<string, string>)[status] || status;
}

function statusTagType(status: string) {
    return ({ ACTIVE: 'success', PENDING: 'warning', DISABLED: 'info', LEFT: 'info', LOCKED: 'danger', INACTIVE: 'info' } as Record<string, string>)[status] || 'info';
}

async function handleMoreCommand(row: SystemUser, command: string) {
    if (command === 'save') await saveUser(row);
    if (command === 'handover') await checkHandover(row);
    if (command === 'lifecycle') await completeLifecycle(row);
    if (command === 'password') await resetUserPassword(row);
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 账号查询、列表与编辑操作 -->
    <div class="system-page" v-loading="loading">
        <section class="panel">
            <div class="panel-header">
                <div>
                    <span class="panel-title">账号与交接</span>
                    <p class="panel-subtitle">按“建档 → 激活 → 交接 → 停用/离职”推进，带 * 的字段为必填项</p>
                </div>
            </div>
            <div class="panel-body account-create-flow">
                <div class="account-flow-steps" aria-label="账号办理流程">
                    <span class="account-flow-step is-current"><b>1</b>人员建档</span>
                    <span class="account-flow-arrow">→</span>
                    <span class="account-flow-step"><b>2</b>权限与岗位</span>
                    <span class="account-flow-arrow">→</span>
                    <span class="account-flow-step"><b>3</b>创建后激活</span>
                </div>
                <div class="account-form-grid">
                    <div class="account-form-section">
                        <div class="account-form-section__title">第 1 步 · 人员档案</div>
                        <div class="account-form-section__fields">
                            <label class="account-field"><span>用户名 <em>*</em></span><el-input v-model="userForm.username" placeholder="用于登录" /></label>
                            <label class="account-field"><span>初始密码 <em>*</em></span><el-input v-model="userForm.password" type="password" show-password placeholder="至少 8 位" /><small>创建后账号为“待激活”，激活时还需设置临时密码。</small></label>
                            <label class="account-field"><span>姓名/昵称 <em>*</em></span><el-input v-model="userForm.nickname" placeholder="请输入真实姓名" /></label>
                            <label class="account-field"><span>员工编号</span><el-input v-model="userForm.employeeNo" placeholder="可选" /></label>
                        </div>
                    </div>
                    <div class="account-form-section">
                        <div class="account-form-section__title">第 2 步 · 权限与岗位</div>
                        <div class="account-form-section__fields">
                            <label class="account-field"><span>所属部门</span><el-select v-model="userForm.deptId" clearable placeholder="请选择部门"><el-option v-for="dept in departments.filter(item => isEnabledStatus(item.status))" :key="dept.id" :label="dept.deptName" :value="dept.id" /></el-select></label>
                            <label class="account-field account-field--wide"><span>系统角色</span><el-select v-model="userForm.roleCodes" multiple collapse-tags placeholder="可多选"><el-option v-for="role in roles.filter(item => isEnabledStatus(item.status))" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" /></el-select></label>
                            <label class="account-field account-field--wide"><span>岗位</span><el-select v-model="userForm.postIds" multiple collapse-tags placeholder="可多选"><el-option v-for="post in posts.filter(item => isEnabledStatus(String(item.status)))" :key="Number(post.id)" :label="String(post.postName)" :value="Number(post.id)" /></el-select></label>
                        </div>
                    </div>
                    <div class="account-form-submit">
                        <div><strong>第 3 步 · 创建并激活</strong><p>点击创建后，账号会出现在下方列表，待审核后再执行“激活账号”。</p></div>
                    <el-button v-if="auth.can('sys:user:create')" type="primary" @click="createUser">创建人员档案</el-button>
                    </div>
                </div>
            </div>
            <!-- 数据表格 -->
            <el-table :data="users" height="520" size="small">
                <el-table-column prop="username" label="账号" width="130" />
                <el-table-column label="姓名" width="140">
                    <template #default="{ row }">
                        <el-input v-model="row.nickname" size="small" />
                    </template>
                </el-table-column>
                <el-table-column prop="employeeNo" label="员工编号" width="130" />
                <el-table-column label="账号类型" width="100">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.userType === 'EXTERNAL' || row.userType === 'EXTERNAL_LEGACY' ? 'warning' : 'info'">
                            {{ row.userType === 'EXTERNAL' || row.userType === 'EXTERNAL_LEGACY' ? '外部' : '内部' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="部门" width="160">
                    <template #default="{ row }">
                        <el-select v-model="row.deptId" clearable size="small">
                            <el-option
                                v-for="dept in departments.filter(item => isEnabledStatus(item.status))"
                                :key="dept.id"
                                :label="dept.deptName"
                                :value="dept.id"
                            />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="系统角色" min-width="220">
                    <template #default="{ row }">
                        <el-select v-model="row.roles" multiple collapse-tags size="small" :disabled="!auth.can('sys:role:grant')">
                            <el-option
                                v-for="role in roles.filter(item => isEnabledStatus(item.status))"
                                :key="role.roleCode"
                                :label="role.roleName"
                                :value="role.roleCode"
                            />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="岗位" min-width="180">
                    <template #default="{ row }">
                        <el-select v-model="row.postIds" multiple collapse-tags size="small">
                            <el-option
                                v-for="post in posts.filter(item => isEnabledStatus(String(item.status)))"
                                :key="Number(post.id)"
                                :label="String(post.postName)"
                                :value="Number(post.id)"
                            />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                        <div v-if="handoverHints[row.id]" class="handover-hint">{{ handoverHints[row.id] }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="下一步操作" width="250" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="row.status === 'PENDING' && auth.can('sys:user:lifecycle')" link type="success" @click="activateUser(row)">激活账号</el-button>
                        <el-button v-else-if="auth.can('sys:user:update')" link type="primary" @click="saveUser(row)">保存变更</el-button>
                        <el-dropdown v-if="auth.can('sys:user:lifecycle') || auth.can('sys:user:reset-password') || auth.can('sys:user:update')" @command="(command: string) => handleMoreCommand(row, command)">
                            <el-button link>更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="row.status === 'PENDING' && auth.can('sys:user:update')" command="save">保存资料</el-dropdown-item>
                                    <el-dropdown-item v-if="auth.can('sys:user:lifecycle')" command="handover">交接校验</el-dropdown-item>
                                    <el-dropdown-item v-if="row.status === 'ACTIVE' && auth.can('sys:user:lifecycle')" command="lifecycle">停用或离职</el-dropdown-item>
                                    <el-dropdown-item v-if="auth.can('sys:user:reset-password')" command="password">重置密码</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="userPagination.pageNo"
                :page-size="userPagination.pageSize"
                :total="userPagination.total"
                :loading="userPagination.loading"
                @update:page-no="userPagination.goTo"
                @update:page-size="userPagination.changePageSize"
            />
        </section>
        <el-dialog v-model="lifecycleVisible" title="完成账号交接" width="460px">
            <template v-if="lifecycleTarget">
                <p class="lifecycle-dialog__intro">账号 <strong>{{ lifecycleTarget.username }}</strong> 的交接校验结果：</p>
                <el-alert v-if="lifecycleOutstanding.length" type="warning" :closable="false" title="仍有未完成交接事项，请处理后再提交" />
                <el-alert v-else type="success" :closable="false" title="交接校验已通过，可以完成生命周期变更" />
                <ul v-if="lifecycleOutstanding.length" class="lifecycle-dialog__list"><li v-for="item in lifecycleOutstanding" :key="item">{{ item }}</li></ul>
                <div class="lifecycle-dialog__choice">
                    <span>完成后状态</span>
                    <el-radio-group v-model="lifecycleStatus" :disabled="lifecycleOutstanding.length > 0">
                        <el-radio-button label="DISABLED">停用账号</el-radio-button>
                        <el-radio-button label="LEFT">标记离职</el-radio-button>
                    </el-radio-group>
                </div>
            </template>
            <template #footer><el-button @click="lifecycleVisible = false">取消</el-button><el-button type="primary" :disabled="lifecycleOutstanding.length > 0" @click="confirmLifecycle">确认变更</el-button></template>
        </el-dialog>
    </div>
</template>
