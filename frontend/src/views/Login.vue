<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">个人中心</h1>
      <p class="login-subtitle">请登录您的账号</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <!-- 用户名 -->
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
          />
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="loading"
          />
          <span class="password-hint">密码需至少 8 位，包含字母和数字</span>
        </div>

        <!-- 验证码 -->
        <div class="form-group captcha-group">
          <label for="captchaCode">验证码</label>
          <div class="captcha-row">
            <input
              id="captchaCode"
              v-model="form.captchaCode"
              type="text"
              placeholder="请输入验证码"
              autocomplete="off"
              :disabled="loading"
            />
            <div class="captcha-image-wrapper" @click="refreshCaptcha" title="点击刷新验证码">
              <img
                v-if="captcha.image"
                :src="captcha.image"
                alt="验证码"
                class="captcha-image"
              />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message" role="alert">
          {{ errorMessage }}
        </div>

        <!-- 提交按钮 -->
        <button type="submit" class="login-btn" :disabled="loading || !isFormValid">
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <!-- 跳转注册 -->
        <p class="register-link">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, login } from '@/api/auth'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const captcha = reactive({
  uuid: '',
  image: ''
})

const loading = ref(false)
const errorMessage = ref('')

const isFormValid = computed(() =>
  form.username.trim() !== '' &&
  form.password !== '' &&
  form.captchaCode.trim() !== ''
)

const ERROR_MESSAGES = {
  CAPTCHA_INVALID: '验证码错误或已过期，请刷新后重试',
  INVALID_CREDENTIALS: '用户名或密码错误',
  ACCOUNT_LOCKED: '账号已被锁定，请 30 分钟后再试',
  ACCOUNT_DISABLED: '账号已被禁用，请联系管理员'
}

async function refreshCaptcha() {
  captcha.image = ''
  captcha.uuid = ''
  errorMessage.value = ''
  try {
    const res = await getCaptcha()
    captcha.uuid = res.data.data.uuid
    captcha.image = res.data.data.image
  } catch {
    errorMessage.value = '验证码加载失败，请刷新页面'
  }
}

async function handleLogin() {
  if (!isFormValid.value || loading.value) return

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await login({
      username: form.username,
      password: form.password,
      captchaUuid: captcha.uuid,
      captchaCode: form.captchaCode
    })

    const { accessToken, refreshToken, user } = res.data.data
    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', refreshToken)
    if (user) localStorage.setItem('user_info', JSON.stringify(user))

    router.push('/app/dashboard')
  } catch (err) {
    const code = err?.response?.data?.code
    errorMessage.value = ERROR_MESSAGES[code] || '登录失败，请稍后重试'

    // 验证码错误后自动刷新验证码
    if (code === 'CAPTCHA_INVALID') {
      form.captchaCode = ''
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a3a6b 0%, #2d6a9f 100%);
  padding: 16px;
}

.login-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a6b;
  margin: 0 0 6px;
  text-align: center;
}

.login-subtitle {
  font-size: 14px;
  color: #888;
  text-align: center;
  margin: 0 0 28px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #2d6a9f;
  box-shadow: 0 0 0 3px rgba(45, 106, 159, 0.15);
}

.form-group input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-row input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
}

.captcha-row input:focus {
  border-color: #2d6a9f;
  box-shadow: 0 0 0 3px rgba(45, 106, 159, 0.15);
}

.captcha-row input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.captcha-image-wrapper {
  width: 110px;
  height: 40px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.captcha-image-wrapper:hover {
  opacity: 0.85;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-loading {
  font-size: 12px;
  color: #aaa;
}

.error-message {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #cf1322;
}

.login-btn {
  height: 42px;
  background: #1a3a6b;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  background: #2d6a9f;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  font-size: 13px;
  color: #888;
  margin: 0;
}

.register-link a {
  color: #2d6a9f;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

.password-hint {
  font-size: 12px;
  color: #aaa;
}
</style>
