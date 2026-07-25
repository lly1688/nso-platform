<template>
  <view class="page">
    <view class="user-card">
      <image class="avatar" src="/static/logo.png" />
      <view class="name">{{ userInfo.nickName || userInfo.username || '未登录' }}</view>
      <view class="role">{{ userInfo.role || '' }}</view>
    </view>

    <view class="menu-list">
      <view class="menu-item" @tap="goPage('/pages/project/list')">
        <text>我的项目</text><text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="goPage('/pages/task/list')">
        <text>我的任务</text><text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="goPage('/pages/change/apply')">
        <text>变更记录</text><text class="arrow">></text>
      </view>
    </view>

    <view class="menu-list" style="margin-top: 24rpx;">
      <view class="menu-item" @tap="handleLogout">
        <text style="color: #ff4d4f;">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { clearAuth, getUserInfo } from '@/utils/auth'
import { getUserInfo as fetchUserInfo } from '@/api/user'

export default {
  data() {
    return {
      userInfo: {}
    }
  },
  onShow() {
    const local = getUserInfo()
    if (local) {
      this.userInfo = local
    }
    this.loadUserInfo()
  },
  methods: {
    async loadUserInfo() {
      try {
        const info = await fetchUserInfo()
        this.userInfo = info
      } catch (e) {
        // ignored
      }
    },
    goPage(url) {
      uni.navigateTo({ url })
    },
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录?',
        success: (res) => {
          if (res.confirm) {
            clearAuth()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; }
.user-card {
  background: linear-gradient(135deg, #007aff, #0051d5);
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; margin-bottom: 20rpx; }
.name { font-size: 36rpx; font-weight: bold; }
.role { font-size: 24rpx; opacity: 0.8; margin-top: 8rpx; }

.menu-list {
  margin: 24rpx;
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
  font-size: 28rpx;
}
.menu-item:last-child { border-bottom: none; }
.arrow { color: #ccc; }
</style>
