<template>
  <div class="stack">
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">基础字典</span>
      </div>
      <el-table
        :data="[
          { type: 'project_status', code: 'TECH_PUBLISHED', label: '技术包已发布' },
          { type: 'sample_status', code: 'WAIT_CUSTOMER_CONFIRM', label: '待客户确认' },
          { type: 'change_status', code: 'EXECUTING', label: '执行中' },
          { type: 'risk_level', code: 'HIGH', label: '高风险' }
        ]"
      >
        <el-table-column prop="type" label="字典类型" width="220" />
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="label" label="名称" />
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">MVP 验收口径</span>
      </div>
      <div class="panel-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="接口前缀">/api/v1/admin、/api/v1/mp、/api/v1/public</el-descriptions-item>
          <el-descriptions-item label="技术栈">Spring Boot 3.5、Vue 3、uni-app</el-descriptions-item>
          <el-descriptions-item label="规则阻断">版本不一致、样品未确认、变更影响未反馈</el-descriptions-item>
          <el-descriptions-item label="演示账号">admin / admin123</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>
