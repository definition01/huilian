<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <div class="logo">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="8" fill="#006EFF"/>
            <path d="M10 15C10 13.3431 11.3431 12 13 12H20C21.6569 12 23 13.3431 23 15V22C23 23.6569 21.6569 25 20 25H13C11.3431 25 10 23.6569 10 22V15Z" fill="white"/>
            <path d="M21 17C21 15.8954 21.8954 15 23 15H27C28.1046 15 29 15.8954 29 17V21C29 22.1046 28.1046 23 27 23H23C21.8954 23 21 22.1046 21 21V17Z" fill="white" fill-opacity="0.7"/>
          </svg>
          <span class="title">会联</span>
        </div>
        <p class="subtitle">注册新账号</p>
      </div>

      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left">
        <n-form-item path="username">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" size="large" />
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="请输入密码" size="large" />
        </n-form-item>
        <n-form-item path="mobile">
          <n-input v-model:value="formData.mobile" placeholder="请输入手机号" size="large">
            <template #prefix>+86</template>
          </n-input>
        </n-form-item>
        <n-form-item path="email">
          <n-input v-model:value="formData.email" placeholder="请输入邮箱" size="large" />
        </n-form-item>
      </n-form>

      <n-button type="primary" block size="large" :loading="loading" @click="handleRegister">
        注册
      </n-button>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)

const formData = ref({
  username: '',
  password: '',
  mobile: '',
  email: ''
})

// 与后端 RegisterDTO 的校验规则保持一致，避免提交后才报错
const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$/,
      message: '密码至少8位，且必须包含大小写字母和数字',
      trigger: 'blur'
    }
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 +86 中国手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ]
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.register({
      ...formData.value,
      mobile: `+86${formData.value.mobile}`
    })
    message.success('注册成功')
    router.push('/home')
  } catch (e) {
    message.error(e.message || e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #006EFF 100%);
}

.register-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-header {
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

.register-footer {
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
