<template>
  <view class="login-page">
    <view class="login-card">
      <view class="title">智慧非标订单平台</view>
      <view class="subtitle">打样与变更协同</view>
      <view class="form">
        <input class="input" v-model="username" placeholder="请输入用户名" />
        <input class="input" v-model="password" type="password" placeholder="请输入密码" />
        <button class="btn" :loading="loading" @tap="handleLogin">登 录</button>
      </view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/user'
import { setToken, setUserInfo } from '@/utils/auth'

export default {
  data() {
    return {
      username: '',
      password: '',
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const res = await login({ username: this.username, password: this.password })
        setToken(res.token)
        setUserInfo(res.user)
        uni.reLaunch({ url: '/pages/index/index' })
      } catch (e) {
        // 错误已在 request 中处理
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
.login-card {
  width: 600rpx;
  padding: 60rpx 40rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.08);
}
.title {
  font-size: 40rpx;
  font-weight: bold;
  text-align: center;
  color: #333;
}
.subtitle {
  font-size: 28rpx;
  text-align: center;
  color: #999;
  margin-top: 12rpx;
  margin-bottom: 60rpx;
}
.input {
  border: 1px solid #e0e0e0;
  border-radius: 8rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  font-size: 28rpx;
}
.btn {
  margin-top: 40rpx;
  background: #007aff;
  color: #fff;
  border-radius: 8rpx;
  font-size: 32rpx;
}
</style>
