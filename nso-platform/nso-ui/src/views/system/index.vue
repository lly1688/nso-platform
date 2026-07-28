<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import type { AuditLog, ImportTask, RuleParam } from '@/types'

const rules = ref<RuleParam[]>([])
const imports = ref<ImportTask[]>([])
const audits = ref<AuditLog[]>([])
const jobs = ref<Record<string, unknown>[]>([])
const users = ref<Record<string, unknown>[]>([])
const roles = ref<Record<string, unknown>[]>([])
const departments = ref<Record<string, unknown>[]>([])
const menus = ref<Record<string, unknown>[]>([])
const dictionaries = ref<Record<string, unknown>[]>([])
const ruleForm = reactive({
  ruleCode: 'RISK_SAMPLE_CONFIRM',
  ruleName: '样品确认临期预警',
  params: { daysBefore: 3, score: 25 },
  status: 'DRAFT'
})
const importRows = ref('[{"name":"测试客户","industry":"非标制造","contactName":"联系人","phone":"13800000000"}]')
const userForm = reactive({ username: '', password: '', nickname: '', roleCodes: [] as string[] })
const roleForm = reactive({ roleCode: '', roleName: '' })
const selectedRoleId = ref<number | null>(null)
const selectedPermissionCodes = ref<string[]>([])
const permissionCatalog = ref<Record<string, unknown>[]>([])
const deptForm = reactive({ deptName: '', leaderName: '' })
const menuForm = reactive({ menuName: '', routePath: '', permissionCode: '' })
const dictForm = reactive({ type: '', code: '', label: '', sortNo: 0 })

async function load() {
  const [ruleData, importData, auditData, jobData, userData, roleData, deptData, menuData, dictData] = await Promise.all([
    api.rules(),
    api.imports(),
    api.auditLogs(),
    api.jobs(),
    api.systemUsers(),
    api.systemRoles(),
    api.departments(),
    api.systemMenus(),
    api.dictionaries()
  ])
  rules.value = ruleData.list
  imports.value = importData.list
  audits.value = auditData.list
  jobs.value = (jobData as any).list || []
  users.value = userData
  roles.value = roleData
  departments.value = deptData
  menus.value = menuData
  dictionaries.value = dictData
}

async function saveRule() {
  await api.saveRule(ruleForm)
  ElMessage.success('规则参数版本已保存')
  await load()
}

async function publishRule(row: RuleParam) {
  await api.publishRule(row.id)
  ElMessage.success('规则参数已发布')
  await load()
}

async function importCustomerRows() {
  try {
    await api.importRows({ importType: 'CUSTOMER', mode: 'INSERT_ONLY', rows: JSON.parse(importRows.value) })
    ElMessage.success('导入任务已执行')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入数据格式错误')
  }
}

async function runJob(row: Record<string, unknown>) {
  await api.runJob(Number(row.id))
  ElMessage.success('定时任务已手动执行')
}

async function createUser() {
  if (!userForm.username.trim() || userForm.password.length < 6) return ElMessage.warning('请填写用户名和至少 6 位的初始密码')
  if (userForm.roleCodes.length === 0) return ElMessage.warning('请至少选择一个角色')
  await api.createSystemUser(userForm)
  userForm.username = ''; userForm.password = ''; userForm.nickname = ''
  ElMessage.success('用户已创建')
  await load()
}

async function selectRole(row: Record<string, unknown>) {
  const roleId = Number(row.id)
  if (!roleId) return
  const detail = await api.rolePermissions(roleId)
  selectedRoleId.value = roleId
  selectedPermissionCodes.value = detail.selectedPermissionCodes || []
  permissionCatalog.value = detail.catalog || []
}

async function saveRolePermissions() {
  if (!selectedRoleId.value) return ElMessage.warning('请先选择一个角色')
  await api.replaceRolePermissions(selectedRoleId.value, selectedPermissionCodes.value)
  ElMessage.success('角色权限已保存，相关账号需重新登录')
  await selectRole({ id: selectedRoleId.value })
}

async function createRole() {
  if (!roleForm.roleCode.trim() || !roleForm.roleName.trim()) return ElMessage.warning('请填写角色编码和名称')
  await api.createSystemRole(roleForm)
  roleForm.roleCode = ''; roleForm.roleName = ''
  ElMessage.success('角色已创建')
  await load()
}

async function createDepartment() {
  if (!deptForm.deptName.trim()) return ElMessage.warning('请填写部门名称')
  await api.createDepartment(deptForm)
  deptForm.deptName = ''; deptForm.leaderName = ''
  ElMessage.success('部门已创建')
  await load()
}

async function createMenu() {
  if (!menuForm.menuName.trim() || !menuForm.permissionCode.trim()) return ElMessage.warning('请填写菜单名称和权限编码')
  await api.createSystemMenu(menuForm)
  menuForm.menuName = ''; menuForm.routePath = ''; menuForm.permissionCode = ''
  ElMessage.success('菜单权限已创建')
  await load()
}

async function saveDictionary() {
  if (!dictForm.type.trim() || !dictForm.code.trim() || !dictForm.label.trim()) return ElMessage.warning('请填写字典类型、编码和名称')
  await api.saveDictionary(dictForm)
  dictForm.type = ''; dictForm.code = ''; dictForm.label = ''; dictForm.sortNo = 0
  ElMessage.success('字典项已保存')
  await load()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="stack">
    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">配置中心</span>
        </div>
        <div class="panel-body inline-form">
          <el-input v-model="ruleForm.ruleCode" placeholder="规则编码" />
          <el-input v-model="ruleForm.ruleName" placeholder="规则名称" />
          <el-input v-model="ruleForm.params.daysBefore" placeholder="预警天数" />
          <el-input v-model="ruleForm.params.score" placeholder="风险分" />
          <el-button type="primary" :icon="'DocumentAdd'" @click="saveRule">保存版本</el-button>
        </div>
        <el-table :data="rules" height="300">
          <el-table-column prop="ruleCode" label="规则编码" width="180" />
          <el-table-column prop="ruleName" label="规则名称" min-width="170" />
          <el-table-column prop="ruleVersion" label="版本" width="90" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button link type="success" @click="publishRule(row)">发布</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">数据导入</span>
        </div>
        <div class="panel-body stack">
          <el-input v-model="importRows" type="textarea" :rows="5" />
          <el-button type="primary" :icon="'Upload'" @click="importCustomerRows">导入客户</el-button>
        </div>
        <el-table :data="imports" height="220">
          <el-table-column prop="importType" label="类型" width="110" />
          <el-table-column prop="mode" label="模式" width="130" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="successCount" label="成功" width="90" />
          <el-table-column prop="failCount" label="失败" width="90" />
        </el-table>
      </div>
    </section>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">定时任务白名单</span>
        </div>
        <el-table :data="jobs" height="320">
          <el-table-column prop="jobName" label="任务" width="160" />
          <el-table-column prop="beanName" label="Bean" min-width="180" />
          <el-table-column prop="cronExpression" label="Cron" width="160" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button link type="primary" @click="runJob(row)">执行</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">操作审计</span>
        </div>
        <el-table :data="audits" height="320">
          <el-table-column prop="moduleName" label="模块" width="120" />
          <el-table-column prop="operationType" label="操作" width="120" />
          <el-table-column prop="businessType" label="对象" width="120" />
          <el-table-column prop="summary" label="摘要" min-width="180" />
          <el-table-column prop="result" label="结果" width="90" />
        </el-table>
      </div>
    </section>

    <section class="two-column">
      <div class="panel">
        <div class="panel-header"><span class="panel-title">用户与组织</span></div>
        <div class="panel-body inline-form">
          <el-input v-model="userForm.username" placeholder="用户名" />
          <el-input v-model="userForm.password" type="password" show-password placeholder="初始密码（至少6位）" />
          <el-input v-model="userForm.nickname" placeholder="姓名/昵称" />
          <el-select v-model="userForm.roleCodes" multiple collapse-tags placeholder="选择角色" style="min-width: 220px">
            <el-option v-for="role in roles" :key="String(role.roleCode)" :label="String(role.roleName)" :value="String(role.roleCode)" />
          </el-select>
          <el-button type="primary" @click="createUser">创建用户</el-button>
        </div>
        <el-table :data="users" height="220">
          <el-table-column prop="username" label="账号" width="130" />
          <el-table-column prop="nickname" label="姓名" width="120" />
          <el-table-column prop="roles" label="角色" min-width="160" />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
        <div class="panel-body inline-form">
          <el-input v-model="deptForm.deptName" placeholder="部门名称" />
          <el-input v-model="deptForm.leaderName" placeholder="负责人" />
          <el-button @click="createDepartment">创建部门</el-button>
        </div>
        <el-table :data="departments" height="180">
          <el-table-column prop="deptName" label="部门" min-width="150" />
          <el-table-column prop="leaderName" label="负责人" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header"><span class="panel-title">角色与菜单权限</span></div>
        <div class="panel-body inline-form">
          <el-input v-model="roleForm.roleCode" placeholder="角色编码" />
          <el-input v-model="roleForm.roleName" placeholder="角色名称" />
          <el-button type="primary" @click="createRole">创建角色</el-button>
        </div>
        <el-table :data="roles" height="180" highlight-current-row @row-click="selectRole">
          <el-table-column prop="roleCode" label="角色编码" min-width="150" />
          <el-table-column prop="roleName" label="角色名称" min-width="150" />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
        <div class="panel-body stack">
          <div class="inline-form">
            <el-tag v-if="selectedRoleId" type="primary">已选择角色 #{{ selectedRoleId }}</el-tag>
            <el-button type="primary" :disabled="!selectedRoleId" @click="saveRolePermissions">保存权限</el-button>
          </div>
          <el-checkbox-group v-model="selectedPermissionCodes" class="permission-grid">
            <el-checkbox v-for="menu in permissionCatalog" :key="String(menu.permissionCode)" :label="String(menu.permissionCode)">
              {{ menu.menuName }}（{{ menu.permissionCode }}）
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="panel-body inline-form">
          <el-input v-model="menuForm.menuName" placeholder="菜单名称" />
          <el-input v-model="menuForm.routePath" placeholder="路由路径" />
          <el-input v-model="menuForm.permissionCode" placeholder="权限编码" />
          <el-button @click="createMenu">创建菜单</el-button>
        </div>
        <el-table :data="menus" height="220">
          <el-table-column prop="menuName" label="菜单" min-width="120" />
          <el-table-column prop="routePath" label="路由" min-width="130" />
          <el-table-column prop="permissionCode" label="权限编码" min-width="150" />
        </el-table>
        <div class="panel-body inline-form">
          <el-input v-model="dictForm.type" placeholder="字典类型" />
          <el-input v-model="dictForm.code" placeholder="字典编码" />
          <el-input v-model="dictForm.label" placeholder="字典名称" />
          <el-input-number v-model="dictForm.sortNo" :min="0" placeholder="排序" />
          <el-button @click="saveDictionary">保存字典项</el-button>
        </div>
        <el-table :data="dictionaries" height="180">
          <el-table-column prop="dict_type" label="类型" min-width="120" />
          <el-table-column prop="dict_code" label="编码" min-width="120" />
          <el-table-column prop="dict_label" label="名称" min-width="140" />
          <el-table-column prop="sort_no" label="排序" width="80" />
        </el-table>
      </div>
    </section>
  </div>
</template>
