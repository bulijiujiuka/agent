<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-area">
        <div class="brand-logo">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="8" fill="#fff" fill-opacity="0.15"/>
            <path d="M12 28V16l8-5 8 5v12l-8 5-8-5z" stroke="#fff" stroke-width="1.8" stroke-linejoin="round"/>
            <circle cx="20" cy="20" r="4" stroke="#fff" stroke-width="1.8"/>
          </svg>
          <span class="brand-name">AI 知识工单平台</span>
        </div>
        <div class="brand-text">
          <h2>智能检索，高效协作</h2>
          <p>基于知识库的检索增强生成系统<br/>覆盖问答、工单、文档全流程</p>
        </div>
        <div class="brand-features">
          <div class="feature-item"><span class="dot"></span>知识库语义检索</div>
          <div class="feature-item"><span class="dot"></span>多轮对话记忆</div>
          <div class="feature-item"><span class="dot"></span>工单智能辅助</div>
        </div>
      </div>
      <div class="left-footer">© 2026 AI Knowledge Platform</div>
    </div>

    <div class="login-right">
      <div class="form-wrapper">
        <div class="form-header">
          <h3>{{ mode === 'login' ? '登录' : '创建账号' }}</h3>
          <p class="form-desc">{{ mode === 'login' ? '登录以访问工作台' : '填写以下信息完成注册' }}</p>
        </div>

        <template v-if="mode === 'login'">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="auth-form" hide-required-asterisk>
            <div class="field-label">用户名</div>
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <div class="field-label">密码</div>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large"
                show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleLogin">
              登录
            </el-button>
          </el-form>
          <div class="form-footer">
            <span>还没有账号？</span>
            <a class="link" @click="switchMode('register')">立即注册</a>
          </div>
        </template>

        <template v-if="mode === 'register'">
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="auth-form" hide-required-asterisk label-width="0">
            <div class="field-label">用户名</div>
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="3-20个字符" size="large" />
            </el-form-item>
            <div class="field-label">昵称</div>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="显示名称" size="large" />
            </el-form-item>
            <div class="field-label">密码</div>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少6位" size="large" show-password />
            </el-form-item>
            <div class="field-label">确认密码</div>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" size="large"
                show-password @keyup.enter="handleRegister" />
            </el-form-item>
            <div class="field-row">
              <div class="field-col">
                <div class="field-label">邮箱 <span class="optional">选填</span></div>
                <el-form-item prop="email">
                  <el-input v-model="registerForm.email" placeholder="your@email.com" size="large" />
                </el-form-item>
              </div>
              <div class="field-col">
                <div class="field-label">手机 <span class="optional">选填</span></div>
                <el-form-item prop="phone">
                  <el-input v-model="registerForm.phone" placeholder="11位手机号" size="large" maxlength="11" />
                </el-form-item>
              </div>
            </div>
            <el-button type="primary" size="large" :loading="regLoading" class="submit-btn" @click="handleRegister">
              注册
            </el-button>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <a class="link" @click="switchMode('login')">返回登录</a>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, UserFilled, Message, Phone } from '@element-plus/icons-vue' // eslint-disable-line no-unused-vars
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
const regLoading = ref(false)
const loginRef = ref()
const registerRef = ref()

const switchMode = (target) => {
  mode.value = target
}

// ===== 登录 =====
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginRef.value) return
  await loginRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  })
}

// ===== 注册 =====
const registerForm = reactive({
  username: '', nickname: '', password: '', confirmPassword: '', email: '', phone: ''
})

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为6-30个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerRef.value) return
  await registerRef.value.validate(async (valid) => {
    if (!valid) return
    regLoading.value = true
    try {
      await register(registerForm)
      ElMessage.success('注册成功，请登录')
      Object.assign(registerForm, { username: '', nickname: '', password: '', confirmPassword: '', email: '', phone: '' })
      mode.value = 'login'
    } catch (error) {
      console.error(error)
    } finally {
      regLoading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.login-left {
  flex: 0 0 440px;
  background: #1a1f36;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 40px 32px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -80px;
    right: -80px;
    width: 260px;
    height: 260px;
    border-radius: 50%;
    background: rgba(99, 102, 241, 0.08);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -40px;
    left: -40px;
    width: 180px;
    height: 180px;
    border-radius: 50%;
    background: rgba(99, 102, 241, 0.06);
  }
}

.brand-area {
  position: relative;
  z-index: 1;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 56px;

  svg {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
  }

  .brand-name {
    font-size: 16px;
    font-weight: 500;
    letter-spacing: 0.5px;
    opacity: 0.9;
  }
}

.brand-text {
  margin-bottom: 48px;

  h2 {
    font-size: 28px;
    font-weight: 600;
    line-height: 1.3;
    margin: 0 0 16px;
    letter-spacing: -0.5px;
  }

  p {
    font-size: 14px;
    line-height: 1.7;
    color: rgba(255, 255, 255, 0.55);
    margin: 0;
  }
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .feature-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.7);
  }

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #6366f1;
    flex-shrink: 0;
  }
}

.left-footer {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
  position: relative;
  z-index: 1;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fb;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;

  h3 {
    font-size: 22px;
    font-weight: 600;
    color: #1a1f36;
    margin: 0 0 8px;
  }

  .form-desc {
    font-size: 14px;
    color: #8c8fa3;
    margin: 0;
  }
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #4a4d65;
  margin-bottom: 6px;

  .optional {
    font-weight: 400;
    color: #b0b3c6;
    font-size: 12px;
  }
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
    box-shadow: 0 0 0 1px #dcdfe6;
    padding: 4px 12px;

    &:hover {
      box-shadow: 0 0 0 1px #b0b3c6;
    }

    &.is-focus {
      box-shadow: 0 0 0 1.5px #6366f1;
    }
  }

  :deep(.el-input--large .el-input__inner) {
    height: 40px;
    font-size: 14px;
  }
}

.field-row {
  display: flex;
  gap: 16px;

  .field-col {
    flex: 1;
    min-width: 0;
  }
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 8px;
  background: #6366f1;
  border-color: #6366f1;
  margin-top: 4px;

  &:hover, &:focus {
    background: #5558e6;
    border-color: #5558e6;
  }
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #8c8fa3;

  .link {
    color: #6366f1;
    cursor: pointer;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

@media (max-width: 840px) {
  .login-left {
    display: none;
  }

  .login-right {
    padding: 24px;
  }
}
</style>
