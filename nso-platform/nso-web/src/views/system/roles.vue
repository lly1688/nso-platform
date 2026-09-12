<script setup lang="ts">
// 角色、菜单与数据范围维护状态。
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { SystemRole } from '@/types';
import { dataScopeOptions, emptyPage, enabledStatusOptions, optional, pageList } from './shared';

const loading = ref(false);
const roles = ref<SystemRole[]>([]);
const menus = ref<Array<Record<string, unknown>>>([]);
const selectedRoleId = ref<number | null>(null);
const selectedPermissionCodes = ref<string[]>([]);
const permissionCatalog = ref<Array<Record<string, unknown>>>([]);
const roleForm = reactive<{
    roleCode: string;
    roleName: string;
    dataScope: SystemRole['dataScope'];
}>({
    roleCode: '',
    roleName: '',
    dataScope: 'SELF'
});

const menuForm = reactive({ menuName: '', routePath: '', permissionCode: '' });
const rolePagination = usePagination<SystemRole>();
const menuPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'menu' });
rolePagination.configure(
    (params) => api.systemRoles(params),
    (result) => {
        roles.value = result.list;
    }
);
menuPagination.configure(
    (params) => api.systemMenus(params),
    (result) => {
        menus.value = result.list;
    }
);

async function load() {
    loading.value = true;
    try {
        await Promise.all([rolePagination.reload(), menuPagination.reload()]);
    }
    finally {
        loading.value = false;
    }
}

async function selectRole(row: SystemRole | Record<string, unknown>) {
    const roleId = Number(row.id);
    if (!roleId) {
        return;
    }
    const detail = await api.rolePermissions(roleId);
    selectedRoleId.value = roleId;
    selectedPermissionCodes.value = detail.selectedPermissionCodes || [];
    permissionCatalog.value = detail.catalog || [];
}

async function createRole() {
    if (!roleForm.roleCode.trim() || !roleForm.roleName.trim()) {
        return ElMessage.warning('请填写角色编码和名称');
    }
    await api.createSystemRole({ ...roleForm });
    roleForm.roleCode = '';
    roleForm.roleName = '';
    roleForm.dataScope = 'SELF';
    ElMessage.success('角色已创建');
    await load();
}

async function saveRole(row: SystemRole) {
    try {
        await api.updateSystemRole(row.id, { roleName: row.roleName, dataScope: row.dataScope, status: row.status });
        ElMessage.success('角色数据范围已保存；影响列表、看板和报表可见性');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '角色保存失败');
    }
}

async function saveRolePermissions() {
    if (!selectedRoleId.value) {
        return ElMessage.warning('请先选择一个角色');
    }
    await api.replaceRolePermissions(selectedRoleId.value, selectedPermissionCodes.value);
    ElMessage.success('角色权限已保存，相关账号需重新登录');
    await selectRole({ id: selectedRoleId.value });
}

async function createMenu() {
    if (!menuForm.menuName.trim() || !menuForm.permissionCode.trim()) {
        return ElMessage.warning('请填写菜单名称和权限编码');
    }
    await api.createSystemMenu({ ...menuForm });
    menuForm.menuName = '';
    menuForm.routePath = '';
    menuForm.permissionCode = '';
    ElMessage.success('菜单权限已创建');
    await load();
    if (selectedRoleId.value) {
        await selectRole({ id: selectedRoleId.value });
    }
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 角色查询、权限配置与列表 -->
    <div class="system-page system-panel-grid" v-loading="loading">
        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">角色目录</span>
            </div>
            <div class="panel-body system-form">
                <el-input v-model="roleForm.roleCode" placeholder="角色编码" />
                <el-input v-model="roleForm.roleName" placeholder="角色名称" />
                <el-select v-model="roleForm.dataScope" placeholder="数据范围">
                    <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-button type="primary" @click="createRole">创建角色</el-button>
            </div>
            <!-- 数据表格 -->
            <el-table :data="roles" height="560" size="small" highlight-current-row @row-click="selectRole">
                <el-table-column prop="roleCode" label="角色编码" min-width="150" />
                <el-table-column label="角色名称" min-width="150">
                    <template #default="{ row }">
                        <el-input v-model="row.roleName" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="数据范围" width="170">
                    <template #default="{ row }">
                        <el-select v-model="row.dataScope" size="small">
                            <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <el-select v-model="row.status" size="small">
                            <el-option v-for="item in enabledStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="90" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click.stop="saveRole(row)">保存</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="rolePagination.pageNo"
                :page-size="rolePagination.pageSize"
                :total="rolePagination.total"
                :loading="rolePagination.loading"
                @update:page-no="rolePagination.goTo"
                @update:page-size="rolePagination.changePageSize"
            />
        </section>

        <div class="system-side-stack">
            <section class="panel">
                <div class="panel-header">
                    <span class="panel-title">权限勾选</span>
                    <el-button type="primary" :disabled="!selectedRoleId" @click="saveRolePermissions">保存权限</el-button>
                </div>
                <div class="panel-body stack">
                    <el-tag v-if="selectedRoleId" type="primary" effect="plain">已选择角色 #{{ selectedRoleId }}</el-tag>
                    <el-empty v-else description="从角色目录选择一个角色" :image-size="64" />
                    <el-checkbox-group v-if="selectedRoleId" v-model="selectedPermissionCodes" class="permission-grid">
                        <el-checkbox
                            v-for="menu in permissionCatalog"
                            :key="String(menu.permissionCode)"
                            :label="String(menu.permissionCode)"
                            >
                            {{ menu.menuName }}（{{ menu.permissionCode }}）
                        </el-checkbox>
                    </el-checkbox-group>
                </div>
            </section>

            <section class="panel">
                <div class="panel-header">
                    <span class="panel-title">菜单权限</span>
                </div>
                <div class="panel-body system-form">
                    <el-input v-model="menuForm.menuName" placeholder="菜单名称" />
                    <el-input v-model="menuForm.routePath" placeholder="路由路径" />
                    <el-input v-model="menuForm.permissionCode" placeholder="权限编码" />
                    <el-button type="primary" @click="createMenu">创建菜单</el-button>
                </div>
                <el-table :data="menus" height="280" size="small">
                    <el-table-column prop="menuName" label="菜单" min-width="120" />
                    <el-table-column prop="routePath" label="路由" min-width="130" />
                    <el-table-column prop="permissionCode" label="权限编码" min-width="150" />
                </el-table>
                <PaginationBar
                    :page-no="menuPagination.pageNo"
                    :page-size="menuPagination.pageSize"
                    :total="menuPagination.total"
                    :loading="menuPagination.loading"
                    @update:page-no="menuPagination.goTo"
                    @update:page-size="menuPagination.changePageSize"
                />
            </section>
        </div>
    </div>
</template>
