<template>
  <view class="page">
    <view class="tab-bar">
      <view class="tab" :class="{ active: tabIndex === 0 }" @tap="tabIndex = 0">待处理</view>
      <view class="tab" :class="{ active: tabIndex === 1 }" @tap="tabIndex = 1">进行中</view>
      <view class="tab" :class="{ active: tabIndex === 2 }" @tap="tabIndex = 2">已完成</view>
    </view>
    <view class="list">
      <view class="task-card" v-for="item in filteredList" :key="item.id" @tap="goDetail(item)">
        <view class="task-header">
          <text class="task-name">{{ item.name }}</text>
          <text class="task-status" :class="item.status">{{ item.statusText }}</text>
        </view>
        <view class="task-info">
          <text>项目: {{ item.projectName }}</text>
          <text>截止时间: {{ item.deadline }}</text>
        </view>
      </view>
      <view v-if="filteredList.length === 0" class="empty">暂无任务</view>
    </view>
  </view>
</template>

<script>
import { getTaskList } from '@/api/task'

export default {
  data() {
    return {
      tabIndex: 0,
      list: []
    }
  },
  computed: {
    filteredList() {
      const statusMap = ['pending', 'in_progress', 'completed']
      return this.list.filter(item => item.status === statusMap[this.tabIndex])
    }
  },
  onLoad() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      try {
        this.list = await getTaskList()
      } catch (e) {
        // ignored
      }
    },
    goDetail(item) {
      uni.navigateTo({ url: `/pages/task/detail?id=${item.id}` })
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
.task-card {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 12rpx;
  padding: 24rpx;
}
.task-header { display: flex; justify-content: space-between; }
.task-name { font-size: 30rpx; font-weight: bold; color: #333; }
.task-status { font-size: 22rpx; padding: 4rpx 10rpx; border-radius: 4rpx; }
.task-status.pending { background: #fff7e6; color: #fa8c16; }
.task-status.in_progress { background: #e8f4ff; color: #007aff; }
.task-status.completed { background: #e8f8e8; color: #52c41a; }
.task-info { margin-top: 16rpx; }
.task-info text { display: block; font-size: 24rpx; color: #999; line-height: 1.8; }
.empty { text-align: center; padding: 100rpx 0; color: #999; }
</style>
