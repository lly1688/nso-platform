<template>
  <view class="page">
    <view class="section" v-if="detail">
      <view class="section-title">基本信息</view>
      <view class="info-row"><text class="label">项目名称</text><text class="value">{{ detail.name }}</text></view>
      <view class="info-row"><text class="label">项目编号</text><text class="value">{{ detail.code }}</text></view>
      <view class="info-row"><text class="label">项目状态</text><text class="value">{{ detail.statusText }}</text></view>
      <view class="info-row"><text class="label">创建时间</text><text class="value">{{ detail.createTime }}</text></view>
    </view>

    <view class="section">
      <view class="section-title">操作</view>
      <view class="action-list">
        <view class="action-item" @tap="goPage('/pages/drawing/version')">
          <text>图纸版本管理</text><text class="arrow">></text>
        </view>
        <view class="action-item" @tap="goPage('/pages/sample/confirm')">
          <text>样品确认</text><text class="arrow">></text>
        </view>
        <view class="action-item" @tap="goPage('/pages/change/apply')">
          <text>变更申请</text><text class="arrow">></text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getProjectDetail } from '@/api/project'

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
        this.detail = await getProjectDetail(id)
      } catch (e) {
        // ignored
      }
    },
    goPage(url) {
      uni.navigateTo({ url })
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
  color: #333;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1px solid #f0f0f0;
}
.info-row { display: flex; padding: 12rpx 0; }
.label { width: 160rpx; color: #999; font-size: 26rpx; }
.value { flex: 1; color: #333; font-size: 26rpx; }
.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 28rpx;
}
.action-item:last-child { border-bottom: none; }
.arrow { color: #ccc; }
</style>
