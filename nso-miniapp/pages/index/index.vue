<template>
  <view class="page">
    <view class="header">
      <view class="user-info">
        <image class="avatar" src="/static/logo.png" />
        <view class="welcome">欢迎回来</view>
        <view class="name">{{ userName }}</view>
      </view>
    </view>

    <view class="stats-grid">
      <view class="stat-card" v-for="item in stats" :key="item.key" @tap="goPage(item.url)">
        <view class="stat-num">{{ item.count }}</view>
        <view class="stat-label">{{ item.label }}</view>
      </view>
    </view>

    <view class="menu-list">
      <view class="menu-item" v-for="item in menus" :key="item.key" @tap="goPage(item.url)">
        <text class="menu-text">{{ item.label }}</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
  </view>
</template>

<script>
import { isLogin } from '@/utils/auth'
import { getUserInfo } from '@/api/user'

export default {
  data() {
    return {
      userName: '',
      stats: [
        { key: 'projects', label: '我的项目', count: 0, url: '/pages/project/list' },
        { key: 'tasks', label: '待办任务', count: 0, url: '/pages/task/list' },
        { key: 'changes', label: '变更审批', count: 0, url: '/pages/change/detail' },
        { key: 'samples', label: '待确认样品', count: 0, url: '/pages/sample/confirm' }
      ],
      menus: [
        { key: 'projects', label: '项目管理', url: '/pages/project/list' },
        { key: 'drawings', label: '图纸版本', url: '/pages/drawing/version' },
        { key: 'samples', label: '样品确认', url: '/pages/sample/confirm' },
        { key: 'changes', label: '变更管理', url: '/pages/change/apply' },
        { key: 'tasks', label: '我的任务', url: '/pages/task/list' }
      ]
    }
  },
  onShow() {
    if (!isLogin()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const user = await getUserInfo()
        this.userName = user.nickName || user.username
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
.header {
  background: linear-gradient(135deg, #007aff, #0051d5);
  padding: 60rpx 40rpx;
  color: #fff;
}
.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-bottom: 16rpx;
}
.welcome { font-size: 26rpx; opacity: 0.8; }
.name { font-size: 36rpx; font-weight: bold; margin-top: 8rpx; }

.stats-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 24rpx;
  margin-top: -30rpx;
}
.stat-card {
  width: calc(50% - 12rpx);
  margin: 6rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 30rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}
.stat-num { font-size: 44rpx; font-weight: bold; color: #007aff; }
.stat-label { font-size: 24rpx; color: #999; margin-top: 8rpx; }

.menu-list {
  margin: 0 24rpx;
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
}
.menu-item:last-child { border-bottom: none; }
.menu-text { font-size: 28rpx; color: #333; }
.menu-arrow { color: #ccc; }
</style>
