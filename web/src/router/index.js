import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue')
      },
      {
        path: 'meetings',
        name: 'MeetingList',
        component: () => import('@/views/meeting-list/MeetingListView.vue')
      },
      {
        path: 'chats',
        name: 'ChatList',
        component: () => import('@/views/chat/ChatListView.vue')
      },
      {
        path: 'contacts',
        name: 'Contacts',
        component: () => import('@/views/contact/ContactsView.vue')
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/SearchView.vue')
      },
      {
        path: 'new-friends',
        name: 'FriendRequests',
        component: () => import('@/views/friend/FriendRequestsView.vue')
      },
      {
        path: 'groups',
        redirect: '/contacts'
      },
      {
        path: 'groups/:groupId',
        name: 'GroupChat',
        component: () => import('@/views/group/GroupChatView.vue')
      },
      {
        path: 'friends',
        redirect: '/new-friends'
      },
      {
        path: 'friends/:friendId',
        name: 'PrivateChat',
        component: () => import('@/views/friend/PrivateChatView.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue')
      }
    ]
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue')
      },
      {
        path: 'meetings',
        name: 'AdminMeetings',
        component: () => import('@/views/admin/AdminMeetings.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue')
      },
      {
        path: 'groups',
        name: 'AdminGroups',
        component: () => import('@/views/admin/AdminGroups.vue')
      },
      {
        path: 'appeals',
        name: 'AdminGroupAppeals',
        component: () => import('@/views/admin/AdminGroupAppeals.vue')
      }
    ]
  },
  {
    path: '/meeting/:meetingId',
    name: 'MeetingRoom',
    component: () => import('@/views/meeting/MeetingRoom.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/meeting/MeetingRoomTest.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin) {
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    const role = (userStore.userInfo?.role || '').toUpperCase()
    if (role !== 'ADMIN' && role !== 'SUPER_ADMIN') {
      next('/home')
      return
    }
  }

  next()
})

export default router
