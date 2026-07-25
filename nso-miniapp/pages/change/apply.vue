<template>
  <view class="page">
    <view class="form">
      <view class="form-item">
        <text class="label">关联项目</text>
        <picker :value="projectIndex" :range="projectList" range-key="name" @change="onProjectChange">
          <view class="picker-val">{{ projectList[projectIndex]?.name || '请选择项目' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">变更类型</text>
        <picker :value="typeIndex" :range="changeTypes" @change="typeIndex = $event.detail.value">
          <view class="picker-val">{{ changeTypes[typeIndex] || '请选择变更类型' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">变更原因</text>
        <textarea class="textarea" v-model="reason" placeholder="请输入变更原因" />
      </view>
      <view class="form-item">
        <text class="label">详细说明</text>
        <textarea class="textarea" v-model="description" placeholder="请输入详细说明" />
      </view>
      <button class="submit-btn" :loading="submitting" @tap="handleSubmit">提交申请</button>
    </view>
  </view>
</template>

<script>
import { getProjectList } from '@/api/project'
import { applyChange } from '@/api/change'

export default {
  data() {
    return {
      projectList: [],
      projectIndex: -1,
      changeTypes: ['设计变更', '工艺变更', '材料变更', '其他'],
      typeIndex: 0,
      reason: '',
      description: '',
      submitting: false
    }
  },
  onLoad() {
    this.fetchProjects()
  },
  methods: {
    async fetchProjects() {
      try {
        const res = await getProjectList()
        this.projectList = res.records || res
      } catch (e) {
        // ignored
      }
    },
    onProjectChange(e) {
      this.projectIndex = e.detail.value
    },
    async handleSubmit() {
      if (this.projectIndex < 0) {
        uni.showToast({ title: '请选择项目', icon: 'none' })
        return
      }
      if (!this.reason) {
        uni.showToast({ title: '请输入变更原因', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        await applyChange({
          projectId: this.projectList[this.projectIndex].id,
          changeType: this.changeTypes[this.typeIndex],
          reason: this.reason,
          description: this.description
        })
        uni.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1500)
      } catch (e) {
        // ignored
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; padding: 24rpx; }
.form { background: #fff; border-radius: 12rpx; padding: 24rpx; }
.form-item { padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.form-item:last-of-type { border-bottom: none; }
.label { font-size: 28rpx; color: #333; display: block; margin-bottom: 12rpx; }
.picker-val { color: #007aff; font-size: 28rpx; padding: 12rpx 0; }
.textarea { width: 100%; min-height: 120rpx; background: #f5f5f5; border-radius: 8rpx; padding: 16rpx; font-size: 26rpx; }
.submit-btn { margin-top: 40rpx; background: #007aff; color: #fff; border-radius: 8rpx; }
</style>
