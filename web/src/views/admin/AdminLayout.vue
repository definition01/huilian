<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <svg width="30" height="30" viewBox="0 0 40 40" fill="none">
          <rect width="40" height="40" rx="8" fill="#006EFF"/>
          <path d="M10 15C10 13.3431 11.3431 12 13 12H20C21.6569 12 23 13.3431 23 15V22C23 23.6569 21.6569 25 20 25H13C11.3431 25 10 23.6569 10 22V15Z" fill="white"/>
          <path d="M21 17C21 15.8954 21.8954 15 23 15H27C28.1046 15 29 15.8954 29 17V21C29 22.1046 28.1046 23 27 23H23C21.8954 23 21 22.1046 21 21V17Z" fill="white" fill-opacity="0.7"/>
        </svg>
        <span>管理后台</span>
      </div>

      <nav class="admin-nav">
        <router-link to="/admin/dashboard" class="admin-nav-item" active-class="active">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
          </svg>
          <span>数据概览</span>
        </router-link>
        <router-link to="/admin/meetings" class="admin-nav-item" active-class="active">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          <span>会议管理</span>
        </router-link>
        <router-link to="/admin/users" class="admin-nav-item" active-class="active">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/groups" class="admin-nav-item" active-class="active">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
          </svg>
          <span>群聊管理</span>
        </router-link>
        <router-link to="/admin/appeals" class="admin-nav-item" active-class="active">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="8" y1="13" x2="16" y2="13"/>
            <line x1="8" y1="17" x2="16" y2="17"/>
          </svg>
          <span>群聊申诉</span>
        </router-link>
      </nav>

      <div class="admin-sidebar-footer">
        <div class="admin-user" v-if="userStore.userInfo">
          <div class="admin-avatar">{{ userStore.userInfo.username?.[0] || 'A' }}</div>
          <div class="admin-user-meta">
            <span class="admin-username">{{ userStore.userInfo.username }}</span>
            <span class="admin-role">管理员</span>
          </div>
        </div>
        <router-link to="/home" class="back-front">返回前台</router-link>
      </div>
    </aside>

    <main class="admin-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}

.admin-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 22px 20px;
  color: #1f2937;
  font-size: 17px;
  font-weight: 600;
  border-bottom: 1px solid #f0f2f5;
}

.admin-nav {
  flex: 1;
  padding: 16px 12px;
}

.admin-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  margin-bottom: 4px;
  border-radius: 8px;
  color: #4b5563;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;

  &:hover {
    background: #f2f6ff;
    color: #006EFF;
  }

  &.active {
    background: #eaf2ff;
    color: #006EFF;
    font-weight: 500;
  }
}

.admin-sidebar-footer {
  padding: 16px 18px;
  border-top: 1px solid #f0f2f5;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.admin-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #006EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
}

.admin-user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-username {
  font-size: 13px;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-role {
  font-size: 12px;
  color: #9ca3af;
}

.back-front {
  display: block;
  text-align: center;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #4b5563;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;

  &:hover {
    border-color: #006EFF;
    color: #006EFF;
  }
}

.admin-content {
  flex: 1;
  overflow: auto;
  padding: 24px 28px;
}
</style>
