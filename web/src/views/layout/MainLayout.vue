<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <svg width="32" height="32" viewBox="0 0 40 40" fill="none">
          <rect width="40" height="40" rx="8" fill="#006EFF"/>
          <path d="M10 15C10 13.3431 11.3431 12 13 12H20C21.6569 12 23 13.3431 23 15V22C23 23.6569 21.6569 25 20 25H13C11.3431 25 10 23.6569 10 22V15Z" fill="white"/>
          <path d="M21 17C21 15.8954 21.8954 15 23 15H27C28.1046 15 29 15.8954 29 17V21C29 22.1046 28.1046 23 27 23H23C21.8954 23 21 22.1046 21 21V17Z" fill="white" fill-opacity="0.7"/>
        </svg>
        <span>会联</span>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/home" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <span>首页</span>
        </router-link>
        <router-link to="/meetings" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          <span>会议列表</span>
        </router-link>
        <router-link to="/chats" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
          </svg>
          <span>聊天</span>
        </router-link>
        <router-link to="/contacts" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>通讯录</span>
        </router-link>
        <router-link to="/search" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <span>搜索</span>
        </router-link>
        <router-link v-if="isAdmin" to="/admin" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
          </svg>
          <span>管理后台</span>
        </router-link>
        <router-link to="/profile" class="nav-item" active-class="active">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          <span>个人中心</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info" v-if="userStore.userInfo">
          <img
            v-if="userStore.userInfo.avatar"
            :src="resolveAvatar(userStore.userInfo.avatar)"
            class="avatar-img"
            alt=""
          />
          <div v-else class="avatar">{{ userStore.userInfo.username?.[0] || 'U' }}</div>
          <span class="username">{{ userStore.userInfo.username }}</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { resolveAvatar } from '@/utils/avatar'

const userStore = useUserStore()

const isAdmin = computed(() => {
  const role = (userStore.userInfo?.role || '').toUpperCase()
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
})

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 24px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-nav {
  flex: 1;
  padding: 0 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: 8px;
  color: #a0a0b8;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
  }

  &.active {
    background: #006EFF;
    color: #fff;
  }
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar,
.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}

.avatar-img {
  object-fit: cover;
  background: #1f2937;
  display: block;
}

.avatar {
  background: #006EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}

.username {
  color: #fff;
  font-size: 14px;
}

.main-content {
  flex: 1;
  overflow: auto;
  background: #f5f7fa;
}
</style>
