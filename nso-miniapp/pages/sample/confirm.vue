<template>
  <view class="page">
    <view class="tab-bar">
      <view class="tab" :class="{ active: tabIndex === 0 }" @tap="tabIndex = 0">待确认</view>
      <view class="tab" :class="{ active: tabIndex === 1 }" @tap="tabIndex = 1">已确认</view>
    </view>
    <view class="list">
      <view class="sample-card" v-for="item in filteredList" :key="item.id">
        <view class="sample-header">
          <text class="sample-name">{{ item.name }}</text>
          <text class="sample-status" :class="item.status">{{ item.statusText }}</text>
        </view>
        <view class="sample-info">
          <text>项目: {{ item.projectName }}</text>
          <text>提交时间: {{ item.createTime }}</text>
        </view>
        <view class="sample-actions" v-if="item.status === 'pending'">
          <button size="mini" type="primary" @tap="handleConfirm(item, 'approved')">通过</button>
          <button size="mini" type="warn" @tap="handleConfirm(item, 'rejected')">驳回</button>
        </view>
      </view>
      <view v-if="filteredList.length === 0" class="empty">暂无记录</view>
    </view>
  </view>
</template>

<script>
import { getSampleList, confirmSample } from '@/api/sample'

export default {
  data() {
    return {
      tabIndex: 0,
      list: []
    }
  },
  computed: {
    filteredList() {
      const status = this.tabIndex === 0 ? 'pending' : 'confirmed'
      return this.list.filter(item => {
        if (this.tabIndex === 1) return item.status !== 'pending'
        return item.status === status
      })
    }
  },
  onLoad() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      try {
        this.list = await getSampleList()
      } catch (e) {
        // ignored
      }
    },
    async handleConfirm(item, status) {
      try {
        await confirmSample({ sampleId: item.id, status })
        uni.showToast({ title: '操作成功', icon: 'success' })
        this.fetchList()
      } catch (e) {
        // ignored
      }
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; }
.tab-bar { display: flex; background: #fff; }
.tab {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  font-size: 28rpx;
  color: #666;
}
.tab.active { color: #007aff; border-bottom: 2px solid #007aff; }
.sample-card {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 12rpx;
  padding: 24rpx;
}
.sample-header { display: flex; justify-content: space-between; }
.sample-name { font-size: 30rpx; font-weight: bold; }
.sample-status { font-size: 22rpx; padding: 4rpx 10rpx; border-radius: 4rpx; }
.sample-status.pending { background: #fff7e6; color: #fa8c16; }
.sample-status.approved { background: #e8f8e8; color: #52c41a; }
.sample-status.rejected { background: #fff0f0; color: #ff4d4f; }
.sample-info { margin-top: 16rpx; }
.sample-info text { display: block; font-size: 24rpx; color: #999; line-height: 1.8; }
.sample-actions { margin-top: 20rpx; display: flex; gap: 16rpx; justify-content: flex-end; }
.empty { text-align: center; padding: 100rpx 0; color: #999; }
</style>
