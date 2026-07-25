<template>
  <view class="page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索项目" @confirm="onSearch" />
    </view>
    <view class="list">
      <view class="card" v-for="item in list" :key="item.id" @tap="goDetail(item.id)">
        <view class="card-title">{{ item.name }}</view>
        <view class="card-row">
          <text class="tag">{{ item.statusText }}</text>
          <text class="date">{{ item.createTime }}</text>
        </view>
        <view class="card-info">{{ item.description }}</view>
      </view>
      <view v-if="list.length === 0 && !loading" class="empty">暂无项目</view>
    </view>
  </view>
</template>

<script>
import { getProjectList } from '@/api/project'

export default {
  data() {
    return {
      keyword: '',
      list: [],
      loading: false
    }
  },
  onLoad() {
    this.fetchList()
  },
  onPullDownRefresh() {
    this.fetchList().then(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    // 分页加载
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getProjectList({ keyword: this.keyword })
        this.list = res.records || res
      } catch (e) {
        // ignored
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.fetchList()
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/project/detail?id=${id}` })
    }
  }
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; }
.search-bar { padding: 20rpx 24rpx; }
.search-input {
  background: #fff;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.card {
  background: #fff;
  margin: 0 24rpx 16rpx;
  border-radius: 12rpx;
  padding: 24rpx;
}
.card-title { font-size: 32rpx; font-weight: bold; color: #333; }
.card-row { display: flex; align-items: center; margin-top: 12rpx; }
.tag {
  background: #e8f4ff;
  color: #007aff;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}
.date { font-size: 24rpx; color: #999; margin-left: 16rpx; }
.card-info { font-size: 26rpx; color: #666; margin-top: 12rpx; }
.empty { text-align: center; padding: 100rpx 0; color: #999; }
</style>
