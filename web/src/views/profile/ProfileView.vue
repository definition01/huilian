<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>个人中心</h1>
    </div>

    <div class="profile-content">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-edit" title="点击更换头像" @click="pickAvatar">
            <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
            <div v-else class="avatar-large">{{ userStore.userInfo?.username?.[0] || 'U' }}</div>
            <div :class="['avatar-mask', { show: uploading }]">
              {{ uploading ? '上传中...' : '更换头像' }}
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            hidden
            @change="handleAvatarSelected"
          />
          <div class="avatar-info">
            <h3>{{ userStore.userInfo?.username || '未登录' }}</h3>
            <p>手机号: {{ userStore.userInfo?.mobile || '未绑定' }}</p>
            <p>邮箱: {{ userStore.userInfo?.email || '未绑定' }}</p>
          </div>
        </div>

        <n-divider />

        <n-form :model="formData" label-placement="left" label-width="80">
          <n-form-item label="用户名">
            <n-input v-model:value="formData.username" placeholder="请输入用户名" />
          </n-form-item>
          <n-form-item label="手机号">
            <n-input v-model:value="formData.mobile" placeholder="请输入手机号">
              <template #prefix>+86</template>
            </n-input>
          </n-form-item>
          <n-form-item label="邮箱">
            <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
          </n-form-item>
        </n-form>

        <div class="form-actions">
          <n-button type="primary" :loading="saving" @click="handleSave">保存修改</n-button>
        </div>
      </div>

      <div class="profile-card">
        <h3>修改密码</h3>
        <n-form :model="passwordForm" label-placement="left" label-width="80">
          <n-form-item label="旧密码">
            <n-input v-model:value="passwordForm.oldPassword" type="password" show-password-on="click" placeholder="请输入旧密码" />
          </n-form-item>
          <n-form-item label="新密码">
            <n-input v-model:value="passwordForm.newPassword" type="password" show-password-on="click" placeholder="请输入新密码" />
          </n-form-item>
          <n-form-item label="确认密码">
            <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" placeholder="请再次输入新密码" />
          </n-form-item>
        </n-form>

        <div class="form-actions">
          <n-button type="primary" :loading="changingPassword" @click="handleChangePassword">修改密码</n-button>
        </div>
      </div>

      <div class="profile-card danger-zone">
        <h3>账号操作</h3>
        <n-button type="error" ghost @click="handleLogout">退出登录</n-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NDivider, useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { updateUserInfo, changePassword, uploadAvatar } from '@/api/user'
import { resolveAvatar } from '@/utils/avatar'

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()

const saving = ref(false)
const changingPassword = ref(false)
const uploading = ref(false)
const avatarInputRef = ref(null)

const AVATAR_MAX_SIZE = 5 * 1024 * 1024

const avatarUrl = computed(() => resolveAvatar(userStore.userInfo?.avatar))

const formData = ref({
  username: '',
  mobile: '',
  email: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 手机号在界面上只展示 11 位，+86 前缀固定
const MOBILE_PATTERN = /^1[3-9]\d{9}$/
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

function stripCountryCode(mobile) {
  return mobile && mobile.startsWith('+86') ? mobile.slice(3) : (mobile || '')
}

// 用户信息由父组件异步加载，直接进 /profile 时 onMounted 里可能还没拿到，
// 这里监听用户信息变化，加载完成后自动回填表单
watch(
  () => userStore.userInfo,
  (info) => {
    if (info) {
      formData.value.username = info.username || ''
      formData.value.mobile = stripCountryCode(info.mobile)
      formData.value.email = info.email || ''
    }
  },
  { immediate: true }
)

function pickAvatar() {
  if (uploading.value) return
  avatarInputRef.value?.click()
}

async function handleAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }
  if (file.size > AVATAR_MAX_SIZE) {
    message.warning('头像大小不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    await uploadAvatar(file)
    await userStore.fetchUserInfo()
    message.success('头像已更新')
  } catch (e) {
    message.error(e?.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleSave() {
  if (!MOBILE_PATTERN.test(formData.value.mobile)) {
    message.warning('请输入正确的 +86 中国手机号')
    return
  }
  if (!EMAIL_PATTERN.test(formData.value.email)) {
    message.warning('邮箱格式不正确')
    return
  }

  saving.value = true
  try {
    await updateUserInfo({
      username: formData.value.username,
      mobile: `+86${formData.value.mobile}`,
      email: formData.value.email
    })
    await userStore.fetchUserInfo()
    message.success('保存成功')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    message.warning('请填写完整')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    message.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    message.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

function handleLogout() {
  userStore.logout()
  message.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 32px;
  max-width: 700px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0;
  }
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin: 0 0 16px;
  }
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-edit {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background: #f3f4f6;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-large {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #006EFF, #4098FC);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.15s;

  &.show {
    opacity: 1;
  }
}

.avatar-edit:hover .avatar-mask {
  opacity: 1;
}

.avatar-info {
  h3 {
    margin: 0 0 6px;
    font-size: 20px;
  }

  p {
    color: #999;
    margin: 0;
    font-size: 14px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.danger-zone {
  border: 1px solid #fee2e2;
}
</style>
