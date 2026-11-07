<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="8" fill="#006EFF"/>
            <path d="M10 15C10 13.3431 11.3431 12 13 12H20C21.6569 12 23 13.3431 23 15V22C23 23.6569 21.6569 25 20 25H13C11.3431 25 10 23.6569 10 22V15Z" fill="white"/>
            <path d="M21 17C21 15.8954 21.8954 15 23 15H27C28.1046 15 29 15.8954 29 17V21C29 22.1046 28.1046 23 27 23H23C21.8954 23 21 22.1046 21 21V17Z" fill="white" fill-opacity="0.7"/>
          </svg>
          <span class="title">会联</span>
        </div>
        <p class="subtitle">登录账号</p>
      </div>

      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left">
        <n-form-item path="username">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <n-icon :size="18"><UserIcon /></n-icon>
            </template>
          </n-input>
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="请输入密码" size="large" @keyup.enter="handleLogin">
            <template #prefix>
              <n-icon :size="18"><LockIcon /></n-icon>
            </template>
          </n-input>
        </n-form-item>
      </n-form>

      <n-button type="primary" block size="large" :loading="loading" @click="handleLogin">
        登录
      </n-button>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NIcon, useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'

const UserIcon = {
  render: () => h('svg', { width: '18', height: '18', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
    h('circle', { cx: '12', cy: '7', r: '4' })
  ])
}

const LockIcon = {
  render: () => h('svg', { width: '18', height: '18', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('rect', { x: '3', y: '11', width: '18', height: '11', rx: '2', ry: '2' }),
    h('path', { d: 'M7 11V7a5 5 0 0 1 10 0v4' })
  ])
}

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)

const formData = ref({
  username: '',
  password: ''
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login(formData.value.username, formData.value.password)
    message.success('登录成功')
    router.push('/home')
  } catch (e) {
    message.error(e.message || e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #006EFF 100%);
}

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;

  .title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
  }
}

.subtitle {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #999;

  a {
    color: #006EFF;
    text-decoration: none;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
