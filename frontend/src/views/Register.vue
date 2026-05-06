<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="register-title">政务管理系统</h1>
      <p class="register-subtitle">创建新账号</p>

      <form class="register-form" @submit.prevent="handleRegister">
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

        <!-- 真实姓名 -->
        <div class="form-group">
          <label for="realName">真实姓名</label>
          <input
            id="realName"
            v-model="form.realName"
            type="text"
            placeholder="请输入真实姓名"
            autocomplete="name"
            :disabled="loading"
          />
        </div>

        <!-- 手机号 -->
        <div class="form-group">
          <label for="phone">手机号</label>
          <input
            id="phone"
            v-model="form.phone"
            type="tel"
            placeholder="请输入中国大陆手机号"
            autocomplete="tel"
            :disabled="loading"
            @input="validatePhone"
          />
          <span v-if="phoneError" class="field-error">{{ phoneError }}</span>
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="至少8位，需包含字母和数字"
            autocomplete="new-password"
            :disabled="loading"
            @input="validatePassword"
          />
          <div v-if="form.password" class="password-strength">
            <div class="strength-bar">
              <div
                class="strength-fill"
                :class="passwordStrengthClass"
                :style="{ width: passwordStrengthWidth }"
              ></div>
            </div>
            <span class="strength-label" :class="passwordStrengthClass">{{ passwordStrengthLabel }}</span>
          </div>
          <span v-if="passwordError" class="field-error">{{ passwordError }}</span>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message" role="alert">
          {{ errorMessage }}
        </div>

        <!-- 成功提示 -->
        <div v-if="successMessage" class="success-message" role="status">
          {{ successMessage }}
        </div>

        <!-- 提交按钮 -->
        <button type="submit" class="register-btn" :disabled="loading || !isFormValid">
          {{ loading ? '注册中...' : '注 册' }}
        </button>

        <!-- 跳转登录 -->
        <p class="login-link">
          已有账号？<router-link to="/login">立即登录</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  phone: '',
  realName: ''
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const phoneError = ref('')
const passwordError = ref('')

const PHONE_REGEX = /^1[3-9]\d{9}$/

function validatePhone() {
  if (!form.phone) {
    phoneError.value = ''
    return
  }
  phoneError.value = PHONE_REGEX.test(form.phone) ? '' : '请输入有效的中国大陆手机号'
}

function validatePassword() {
  if (!form.password) {
    passwordError.value = ''
    return
  }
  if (form.password.length < 8) {
    passwordError.value = '密码长度不能少于 8 位'
    return
  }
  const hasLetter = /[a-zA-Z]/.test(form.password)
  const hasDigit = /\d/.test(form.password)
  if (!hasLetter || !hasDigit) {
    passwordError.value = '密码必须同时包含字母和数字'
    return
  }
  passwordError.value = ''
}

// 密码强度计算
const passwordStrength = computed(() => {
  const pwd = form.password
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 8) score++
  if (pwd.length >= 12) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++
  return score
})

const passwordStrengthClass = computed(() => {
  const s = passwordStrength.value
  if (s <= 1) return 'weak'
  if (s <= 3) return 'medium'
  return 'strong'
})

const passwordStrengthWidth = computed(() => {
  const s = passwordStrength.value
  return `${Math.min(100, s * 20)}%`
})

const passwordStrengthLabel = computed(() => {
  const s = passwordStrength.value
  if (s <= 1) return '弱'
  if (s <= 3) return '中'
  return '强'
})

const isFormValid = computed(() =>
  form.username.trim() !== '' &&
  form.realName.trim() !== '' &&
  PHONE_REGEX.test(form.phone) &&
  form.password.length >= 8 &&
  /[a-zA-Z]/.test(form.password) &&
  /\d/.test(form.password)
)

const ERROR_MESSAGES = {
  USER_ALREADY_EXISTS: '该用户名已被注册，请更换用户名',
  INVALID_PHONE_FORMAT: '手机号格式不正确，请输入中国大陆手机号',
  WEAK_PASSWORD: '密码强度不足，请使用至少 8 位且包含字母和数字的密码'
}

async function handleRegister() {
  if (!isFormValid.value || loading.value) return

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone,
      realName: form.realName
    })

    successMessage.value = '注册成功！即将跳转到登录页...'
    setTimeout(() => router.push('/login'), 1500)
  } catch (err) {
    const code = err?.response?.data?.code
    errorMessage.value = ERROR_MESSAGES[code] || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a3a6b 0%, #2d6a9f 100%);
  padding: 16px;
}

.register-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.register-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a6b;
  margin: 0 0 6px;
  text-align: center;
}

.register-subtitle {
  font-size: 14px;
  color: #888;
  text-align: center;
  margin: 0 0 28px;
}

.register-form {
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

.field-error {
  font-size: 12px;
  color: #cf1322;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: #e8e8e8;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background-color 0.3s;
}

.strength-fill.weak {
  background: #ff4d4f;
}

.strength-fill.medium {
  background: #faad14;
}

.strength-fill.strong {
  background: #52c41a;
}

.strength-label {
  font-size: 12px;
  min-width: 16px;
}

.strength-label.weak {
  color: #ff4d4f;
}

.strength-label.medium {
  color: #faad14;
}

.strength-label.strong {
  color: #52c41a;
}

.error-message {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #cf1322;
}

.success-message {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #389e0d;
}

.register-btn {
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

.register-btn:hover:not(:disabled) {
  background: #2d6a9f;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  font-size: 13px;
  color: #888;
  margin: 0;
}

.login-link a {
  color: #2d6a9f;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
