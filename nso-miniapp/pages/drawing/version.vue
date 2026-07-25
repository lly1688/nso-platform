<template>
  <view class="page">
    <view class="version-list">
      <view class="version-card" v-for="item in versions" :key="item.id">
        <view class="version-header">
          <text class="version-tag">V{{ item.version }}</text>
          <text class="version-status" :class="item.status">{{ item.statusText }}</text>
        </view>
        <view class="version-info">
          <text>上传人: {{ item.uploader }}</text>
          <text>上传时间: {{ item.createTime }}</text>
        </view>
        <view class="version-files" v-if="item.files">
          <view class="file-item" v-for="file in item.files" :key="file.name">
            <text>{{ file.name }}</text>
          </view>
        </view>
      </view>
      <view v-if="versions.length === 0" class="empty">暂无图纸版本</view>
    </view>
  </view>
</template>

<script>
import { getDrawingVersions } from '@/api/drawing'

export default {
  data() {
    return {
      projectId: '',
      versions: []
    }
  },
  onLoad(options) {
    this.projectId = options.projectId || ''
    this.fetchVersions()
  },
  methods: {
    async fetchVersions() {
      try {
        this.versions = await getDrawingVersions(this.projectId)
      } catch (e) {
        // ignored
      }
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; padding: 20rpx; }
.version-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.version-header { display: flex; justify-content: space-between; align-items: center; }
.version-tag { font-size: 32rpx; font-weight: bold; color: #333; }
.version-status { font-size: 24rpx; padding: 4rpx 12rpx; border-radius: 4rpx; }
.version-status.approved { background: #e8f8e8; color: #52c41a; }
.version-status.pending { background: #fff7e6; color: #fa8c16; }
.version-info { margin-top: 16rpx; }
.version-info text { display: block; font-size: 24rpx; color: #999; line-height: 1.8; }
.version-files { margin-top: 16rpx; }
.file-item {
  background: #f5f5f5;
  padding: 12rpx 16rpx;
  border-radius: 6rpx;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #007aff;
}
.empty { text-align: center; padding: 100rpx 0; color: #999; }
</style>
