<script setup lang="ts">
// 部门、岗位与负责人维护状态。
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PaginationBar from '@/components/PaginationBar.vue';
import { usePagination } from '@/composables/usePagination';
import type { SystemDept, SystemUser } from '@/types';
import { emptyPage, enabledStatusOptions, isEnabledStatus, optional, pageList } from './shared';

const loading = ref(false);
const departments = ref<SystemDept[]>([]);
const users = ref<SystemUser[]>([]);
const posts = ref<Array<Record<string, unknown>>>([]);
const deptForm = reactive({
    deptName: '',
    parentId: undefined as number | undefined,
    leaderUserId: undefined as number | undefined
});

const postForm = reactive({ postCode: '', postName: '', sortNo: 0 });
const departmentPagination = usePagination<SystemDept>({ queryPrefix: 'dept' });
const postPagination = usePagination<Record<string, unknown>>({ queryPrefix: 'post' });
departmentPagination.configure(
    (params) => api.departments(params),
    (result) => {
        departments.value = result.list;
    }
);
postPagination.configure(
    (params) => api.systemPosts(params),
    (result) => {
        posts.value = result.list;
    }
);

async function load() {
    loading.value = true;
    try {
        const [, userData] = await Promise.all([
            departmentPagination.reload(),
            optional(api.systemUsers({ pageSize: 100 }), emptyPage<SystemUser>()),
            postPagination.reload()
        ]);
        users.value = pageList(userData);
    }
    finally {
        loading.value = false;
    }
}

async function createDepartment() {
    if (!deptForm.deptName.trim()) {
        return ElMessage.warning('请填写部门名称');
    }
    await api.createDepartment({ ...deptForm });
    deptForm.deptName = '';
    deptForm.parentId = undefined;
    deptForm.leaderUserId = undefined;
    ElMessage.success('部门已创建');
    await load();
}

async function saveDepartment(row: SystemDept) {
    try {
        await api.updateDepartment(row.id, {
            deptName: row.deptName,
            parentId: row.parentId,
            leaderUserId: row.leaderUserId,
            phone: row.phone,
            sortNo: row.sortNo,
            status: row.status
        });
        ElMessage.success('部门组织关系已保存');
        await load();
    }
    catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '部门保存失败');
    }
}

async function createPost() {
    if (!postForm.postCode.trim() || !postForm.postName.trim()) {
        return ElMessage.warning('请填写岗位编码和名称');
    }
    await api.createSystemPost({ ...postForm });
    postForm.postCode = '';
    postForm.postName = '';
    postForm.sortNo = 0;
    ElMessage.success('岗位已创建');
    await load();
}

onMounted(() => {
    void load();
});
</script>

<template>
    <!-- 组织查询、列表与维护操作 -->
    <div class="system-page system-panel-grid" v-loading="loading">
        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">部门组织</span>
            </div>
            <div class="panel-body system-form">
                <el-input v-model="deptForm.deptName" placeholder="部门名称" />
                <el-select v-model="deptForm.parentId" clearable placeholder="上级部门">
                    <el-option
                        v-for="dept in departments.filter(item => isEnabledStatus(item.status))"
                        :key="dept.id"
                        :label="dept.deptName"
                        :value="dept.id"
                    />
                </el-select>
                <el-select v-model="deptForm.leaderUserId" clearable placeholder="负责人账号">
                    <el-option
                        v-for="user in users.filter(item => isEnabledStatus(item.status) && item.userType === 'INTERNAL')"
                        :key="user.id"
                        :label="user.nickname || user.username"
                        :value="user.id"
                    />
                </el-select>
                <el-button type="primary" @click="createDepartment">创建部门</el-button>
            </div>
            <!-- 数据表格 -->
            <el-table :data="departments" height="520" size="small">
                <el-table-column label="部门" min-width="180">
                    <template #default="{ row }">
                        <el-input v-model="row.deptName" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="上级部门" width="170">
                    <template #default="{ row }">
                        <el-select v-model="row.parentId" clearable size="small">
                            <el-option
                                v-for="dept in departments.filter(item => item.id !== row.id && isEnabledStatus(item.status))"
                                :key="dept.id"
                                :label="dept.deptName"
                                :value="dept.id"
                            />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="负责人账号" width="170">
                    <template #default="{ row }">
                        <el-select v-model="row.leaderUserId" clearable size="small">
                            <el-option
                                v-for="user in users.filter(item => isEnabledStatus(item.status) && item.userType === 'INTERNAL')"
                                :key="user.id"
                                :label="user.nickname || user.username"
                                :value="user.id"
                            />
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
                        <el-button link type="primary" @click="saveDepartment(row)">保存</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <PaginationBar
                :page-no="departmentPagination.pageNo"
                :page-size="departmentPagination.pageSize"
                :total="departmentPagination.total"
                :loading="departmentPagination.loading"
                @update:page-no="departmentPagination.goTo"
                @update:page-size="departmentPagination.changePageSize"
            />
        </section>

        <section class="panel">
            <div class="panel-header">
                <span class="panel-title">岗位目录</span>
            </div>
            <div class="panel-body system-form">
                <el-input v-model="postForm.postCode" placeholder="岗位编码" />
                <el-input v-model="postForm.postName" placeholder="岗位名称" />
                <el-input-number v-model="postForm.sortNo" :min="0" :controls="false" placeholder="排序" class="system-form__small" />
                <el-button type="primary" @click="createPost">创建岗位</el-button>
            </div>
            <el-table :data="posts" height="520" size="small">
                <el-table-column prop="postCode" label="岗位编码" min-width="140" />
                <el-table-column prop="postName" label="岗位名称" min-width="160" />
                <el-table-column prop="status" label="状态" width="100" />
            </el-table>
            <PaginationBar
                :page-no="postPagination.pageNo"
                :page-size="postPagination.pageSize"
                :total="postPagination.total"
                :loading="postPagination.loading"
                @update:page-no="postPagination.goTo"
                @update:page-size="postPagination.changePageSize"
            />
        </section>
    </div>
</template>
