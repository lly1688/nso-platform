<template>
  <view class="page">
    <view class="section" v-if="detail">
      <view class="section-title">变更详情</view>
      <view class="info-row"><text class="label">变更编号</text><text class="value">{{ detail.code }}</text></view>
      <view class="info-row"><text class="label">关联项目</text><text class="value">{{ detail.projectName }}</text></view>
      <view class="info-row"><text class="label">变更类型</text><text class="value">{{ detail.changeType }}</text></view>
      <view class="info-row"><text class="label">当前状态</text><text class="value">{{ detail.statusText }}</text></view>
      <view class="info-row"><text class="label">申请人</text><text class="value">{{ detail.applicant }}</text></view>
      <view class="info-row"><text class="label">申请时间</text><text class="value">{{ detail.createTime }}</text></view>
    </view>

    <view class="section" v-if="detail">
      <view class="section-title">变更原因</view>
      <view class="content">{{ detail.reason }}</view>
    </view>

    <view class="section" v-if="detail">
      <view class="section-title">详细说明</view>
      <view class="content">{{ detail.description }}</view>
    </view>

    <view class="section" v-if="detail && detail.status === 'pending'">
      <view class="action-btns">
        <button type="primary" @tap="handleApprove('approved')">通过</button>
        <button type="warn" @tap="handleApprove('rejected')">驳回</button>
      </view>
    </view>
  </view>
</template>

<script>
import { getChangeDetail, approveChange } from '@/api/change'

export default {
  data() {
    return { detail: null }
  },
  onLoad(options) {
    if (options.id) this.fetchDetail(options.id)
  },
  methods: {
    async fetchDetail(id) {
      try {
        this.detail = await getChangeDetail(id)
      } catch (e) {
        // ignored
      }
    },
    async handleApprove(status) {
      try {
        await approveChange({ changeId: this.detail.id, status })
        uni.showToast({ title: '操作成功', icon: 'success' })
        this.fetchDetail(this.detail.id)
      } catch (e) {
        // ignored
      }
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; padding-bottom: 30rpx; }
.section {
  background: #fff;
  margin: 20rpx 24rpx;
  border-radius: 12rpx;
  padding: 24rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1px solid #f0f0f0;
}
.info-row { display: flex; padding: 12rpx 0; }
.label { width: 160rpx; color: #999; font-size: 26rpx; }
.value { flex: 1; color: #333; font-size: 26rpx; }
.content { font-size: 26rpx; color: #666; line-height: 1.8; }
.action-btns { display: flex; gap: 24rpx; }
.action-btns button { flex: 1; }
</style>
