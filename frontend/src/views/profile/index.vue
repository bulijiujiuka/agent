<template>
  <div class="profile-container">
    <!-- 顶部个人信息横幅 -->
    <div class="profile-header">
      <div class="header-main">
        <div class="avatar-wrapper">
          <el-upload
            class="avatar-uploader"
            action="/api/file/upload"
            :headers="uploadHeaders"
            :data="{ bizType: 'avatar' }"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :on-success="handleAvatarSuccess"
          >
            <div class="avatar-box">
              <el-avatar :size="110" :src="profileForm.avatar || defaultAvatar" />
              <div class="avatar-overlay">
                <el-icon :size="24"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
          </el-upload>
        </div>
        <div class="user-body">
          <div class="user-primary">
            <h2 class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h2>
            <p class="user-account">@{{ userStore.userInfo?.username }}</p>
            <div class="user-tags" v-if="userStore.userInfo?.roleNames?.length">
              <el-tag
                v-for="role in userStore.userInfo.roleNames"
                :key="role"
                size="small"
                round
                class="role-tag"
              >
                {{ role }}
              </el-tag>
            </div>
          </div>
          <div class="user-meta">
            <div class="meta-item">
              <el-icon :size="15"><Message /></el-icon>
              <span>{{ userStore.userInfo?.email || '未设置' }}</span>
            </div>
            <div class="meta-item">
              <el-icon :size="15"><Phone /></el-icon>
              <span>{{ userStore.userInfo?.phone || '未设置' }}</span>
            </div>
            <div class="meta-item">
              <el-icon :size="15"><Calendar /></el-icon>
              <span>加入于 {{ formatDate(userStore.userInfo?.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方 Tabs 卡片 -->
    <el-card class="profile-card" shadow="hover">
      <el-tabs v-model="activeTab">
        <!-- 基本资料 -->
        <el-tab-pane label="基本资料" name="info">
          <div class="tab-content">
            <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="80px" label-position="left">
              <el-form-item label="用户名">
                <el-input :model-value="userStore.userInfo?.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="handleSaveProfile">
                  保存修改
                </el-button>
                <el-button @click="resetProfile">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <div class="tab-content">
            <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="100px" label-position="left">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">确认修改</el-button>
                <el-button @click="resetPwdForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Camera, Message, Phone, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile, changePassword } from '@/api/auth'

const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const activeTab = ref('info')
const saving = ref(false)
const changingPwd = ref(false)
const profileRef = ref()
const pwdRef = ref()

const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

// ===== 基本资料 =====
const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const profileRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const initProfile = () => {
  const info = userStore.userInfo
  if (info) {
    profileForm.nickname = info.nickname || ''
    profileForm.email = info.email || ''
    profileForm.phone = info.phone || ''
    profileForm.avatar = info.avatar || ''
  }
}

const resetProfile = () => initProfile()

const handleSaveProfile = async () => {
  if (!profileRef.value) return
  await profileRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res = await updateProfile(profileForm)
      // 更新本地 store
      userStore.userInfo = { ...userStore.userInfo, ...res.data }
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
      ElMessage.success('个人信息修改成功')
    } finally {
      saving.value = false
    }
  })
}

// ===== 头像上传 =====
const beforeAvatarUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('头像只能是 JPG/PNG/GIF/WEBP 格式'); return false }
  if (!isLt2M) { ElMessage.error('头像大小不能超过 2MB'); return false }
  return true
}

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    profileForm.avatar = response.data.fileUrl
    // 自动保存
    handleSaveProfile()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// ===== 修改密码 =====
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为6-30个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const resetPwdForm = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdRef.value?.resetFields()
}

const handleChangePassword = async () => {
  if (!pwdRef.value) return
  await pwdRef.value.validate(async (valid) => {
    if (!valid) return
    changingPwd.value = true
    try {
      await changePassword({
        username: userStore.userInfo.username,
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      resetPwdForm()
      // 修改密码后退出登录
      setTimeout(() => {
        userStore.logout()
        window.location.href = '/login'
      }, 1500)
    } finally {
      changingPwd.value = false
    }
  })
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => initProfile())
</script>

<style lang="scss" scoped>
.profile-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}

/* ========== 顶部横幅 ========== */
.profile-header {
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgb(64, 158, 255) 0%, rgba(64, 158, 255, 0.8) 50%, rgba(64, 158, 255, 0.6) 100%);
  box-shadow: 0 8px 30px rgba(64, 158, 255, 0.25);
}

.header-main {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 36px 40px;
}

/* ----- 头像 ----- */
.avatar-wrapper {
  flex-shrink: 0;
}

.avatar-uploader {
  line-height: 0;
}

.avatar-box {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 4px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  transition: all 0.3s;

  &:hover {
    border-color: #fff;
    box-shadow: 0 6px 28px rgba(0, 0, 0, 0.3);
    transform: scale(1.04);

    .avatar-overlay {
      opacity: 1;
    }
  }

  :deep(.el-avatar) {
    width: 100% !important;
    height: 100% !important;
  }
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 12px;
  gap: 2px;
}

/* ----- 用户信息主体 ----- */
.user-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.user-primary {
  .user-name {
    font-size: 24px;
    font-weight: 700;
    color: #fff;
    margin: 0 0 4px;
    line-height: 1.3;
    text-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  }

  .user-account {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.75);
    margin: 0 0 10px;
  }

  .user-tags {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .role-tag {
    background: rgba(255, 255, 255, 0.2) !important;
    color: #fff !important;
    border: 1px solid rgba(255, 255, 255, 0.35) !important;
    backdrop-filter: blur(4px);
  }
}

/* ----- 右侧信息 ----- */
.user-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.85);
    white-space: nowrap;

    .el-icon {
      color: rgba(255, 255, 255, 0.6);
    }
  }
}

/* ========== 下方卡片 ========== */
.profile-card {
  border-radius: 16px;

  :deep(.el-card__body) {
    padding: 24px 32px;
  }

  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
  }
}

.tab-content {
  max-width: 520px;

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

/* ========== 响应式 ========== */
@media (max-width: 640px) {
  .header-main {
    flex-direction: column;
    text-align: center;
    padding: 28px 20px;
    gap: 16px;
  }

  .user-body {
    flex-direction: column;
    align-items: center;
  }

  .user-primary {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .user-meta {
    align-items: center;
  }

  .tab-content {
    max-width: 100%;
  }
}
</style>
