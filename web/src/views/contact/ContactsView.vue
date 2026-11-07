<template>
  <div class="contacts-page">
    <div class="page-header">
      <div>
        <h1>通讯录</h1>
        <p>好友与群聊都在这里</p>
      </div>
      <n-button type="primary" size="small" @click="showCreate = true">创建群聊</n-button>
    </div>

    <div class="section-card">
      <div class="entry-row" @click="router.push('/new-friends')">
        <div class="entry-icon">+</div>
        <div class="entry-info">
          <div class="entry-name">新的朋友</div>
          <div class="entry-sub">查看并处理好友申请</div>
        </div>
        <span v-if="pendingCount" class="badge">{{ pendingCount }}</span>
        <span class="chevron">›</span>
      </div>
    </div>

    <div class="section-card">
      <div class="section-title">我的好友（{{ friends.length }}）</div>
      <n-spin :show="loading">
        <div v-if="friends.length">
          <div
            v-for="friend in friends"
            :key="friend.userId"
            class="contact-row"
            @click="enterFriendChat(friend)"
          >
            <img v-if="friend.avatar" :src="resolveAvatar(friend.avatar)" class="avatar-img" alt="" />
            <div v-else class="avatar friend-avatar">{{ (friend.remark || friend.username)?.[0] || 'U' }}</div>
            <div class="contact-info">
              <div class="contact-name">
                {{ friend.remark || friend.username }}
                <span v-if="friend.remark" class="contact-tag">备注</span>
              </div>
              <div class="contact-sub">
                <template v-if="friend.remark">{{ friend.username }} · </template>{{ friend.mobile || '未绑定手机号' }}
              </div>
            </div>
            <n-button size="tiny" quaternary @click.stop="openRemarkDialog(friend)">备注</n-button>
            <span class="chevron">›</span>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="还没有好友，去搜索页添加吧" class="empty" />
      </n-spin>
    </div>

    <div class="section-card">
      <div class="section-title">我的群聊（{{ groups.length }}）</div>
      <n-spin :show="loading">
        <div v-if="groups.length">
          <div
            v-for="group in groups"
            :key="group.id"
            class="contact-row"
            @click="enterGroupChat(group)"
          >
            <div class="avatar group-avatar">{{ group.groupName?.[0] || 'G' }}</div>
            <div class="contact-info">
              <div class="contact-name">
                {{ group.groupName }}
                <span v-if="group.myRole === 'owner'" class="contact-tag owner">群主</span>
              </div>
              <div class="contact-sub">群号 {{ group.groupNo }} · {{ group.memberCount }} 名成员</div>
            </div>
            <span class="chevron">›</span>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="还没有群聊，点击右上角创建" class="empty" />
      </n-spin>
    </div>

    <n-modal v-model:show="showCreate" preset="card" title="创建群聊" class="dialog-card">
      <div class="field">
        <div class="field-label">群名称</div>
        <n-input v-model:value="createForm.groupName" maxlength="100" placeholder="给群聊起个名字" />
      </div>
      <div class="field">
        <div class="field-label">邀请成员</div>
        <n-input
          v-model:value="createForm.memberUsernamesText"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          placeholder="输入用户名，多个用逗号或换行分隔（选填）"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button type="primary" :loading="creating" @click="handleCreate">创建</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="showRemarkDialog" preset="card" title="好友备注" class="dialog-card">
      <n-input v-model:value="remarkInput" maxlength="50" show-count placeholder="输入备注，留空则取消备注" />
      <template #footer>
        <div class="dialog-footer">
          <n-button @click="showRemarkDialog = false">取消</n-button>
          <n-button type="primary" :loading="savingRemark" @click="handleSaveRemark">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NInput, NSpin, NEmpty, NModal, useMessage } from 'naive-ui'
import { createGroup, getMyGroups } from '@/api/group'
import { getFriends, getReceivedFriendRequests, updateFriendRemark } from '@/api/friend'
import { resolveAvatar } from '@/utils/avatar'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const groups = ref([])
const friends = ref([])
const pendingCount = ref(0)

const showCreate = ref(false)
const creating = ref(false)
const createForm = ref({
  groupName: '',
  memberUsernamesText: ''
})

const showRemarkDialog = ref(false)
const remarkTarget = ref(null)
const remarkInput = ref('')
const savingRemark = ref(false)

async function loadData() {
  loading.value = true
  try {
    const [groupRes, friendRes, requestRes] = await Promise.all([
      getMyGroups(),
      getFriends(),
      getReceivedFriendRequests()
    ])
    groups.value = groupRes.data || []
    friends.value = friendRes.data || []
    pendingCount.value = (requestRes.data || []).length
  } catch (e) {
    message.error(e?.message || '加载通讯录失败')
  } finally {
    loading.value = false
  }
}

function enterFriendChat(friend) {
  router.push(`/friends/${friend.userId}`)
}

function enterGroupChat(group) {
  router.push(`/groups/${group.id}`)
}

function parseUsernames(text) {
  if (!text) return []
  return [...new Set(text.split(/[,，\n;；\s]+/).map(name => name.trim()).filter(Boolean))]
}

async function handleCreate() {
  const groupName = createForm.value.groupName.trim()
  if (!groupName) {
    message.warning('请输入群名称')
    return
  }

  creating.value = true
  try {
    const res = await createGroup({
      groupName,
      memberUsernames: parseUsernames(createForm.value.memberUsernamesText)
    })
    message.success('群聊创建成功')
    showCreate.value = false
    createForm.value = { groupName: '', memberUsernamesText: '' }
    await loadData()
    if (res.data?.id) {
      router.push(`/groups/${res.data.id}`)
    }
  } catch (e) {
    message.error(e?.message || '创建群聊失败')
  } finally {
    creating.value = false
  }
}

function openRemarkDialog(friend) {
  remarkTarget.value = friend
  remarkInput.value = friend.remark || ''
  showRemarkDialog.value = true
}

async function handleSaveRemark() {
  if (!remarkTarget.value) return
  savingRemark.value = true
  try {
    await updateFriendRemark(remarkTarget.value.userId, remarkInput.value.trim())
    message.success('备注已保存')
    showRemarkDialog.value = false
    await loadData()
  } catch (e) {
    message.error(e?.message || '保存备注失败')
  } finally {
    savingRemark.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.contacts-page {
  padding: 32px;
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;

  h1 {
    margin: 0;
    font-size: 24px;
    color: #1f2937;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.section-card {
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.section-title {
  padding: 14px 18px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #f3f4f6;
}

.entry-row,
.contact-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
  }
}

.entry-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #ff9f0a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 300;
  flex-shrink: 0;
}

.entry-info {
  flex: 1;
}

.entry-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.entry-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #f1f5f9;
  flex-shrink: 0;
}

.friend-avatar {
  background: #006eff;
}

.group-avatar {
  background: #10b981;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 400;
  vertical-align: middle;

  &.owner {
    background: #ecfdf5;
    color: #10b981;
  }
}

.contact-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  color: #cbd5e1;
  font-size: 16px;
  flex-shrink: 0;
}

.empty {
  padding: 24px 0;
}

.dialog-card {
  width: 420px;
}

.field {
  margin-bottom: 14px;
}

.field-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #374151;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
