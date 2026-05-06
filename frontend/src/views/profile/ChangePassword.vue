<template>
  <div class="page">
    <div class="card">
      <h2>修改密码</h2>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="currentPwd">当前密码</label>
          <input id="currentPwd" v-model="form.currentPassword" type="password"
            placeholder="请输入当前密码" autocomplete="current-password" />
        </div>
        <div class="form-group">
          <label for="newPwd">新密码</label>
          <input id="newPwd" v-model="form.newPassword" type="password"
            placeholder="至少8位，包含字母和数字" autocomplete="new-password"
            @input="validateNew" />
          <div v-if="form.newPassword" class="strength-bar">
            <div class="strength-fill" :class="strengthClass" :style="{ width: strengthWidth }"></div>
          </div>
          <span v-if="newPwdError" class="field-error">{{ newPwdError }}</span>
        </div>
        <div class="form-group">
          <label for="confirmPwd">确认新密码</label>
          <input id="confirmPwd" v-model="form.confirmPassword" type="password"
            placeholder="再次输入新密码" autocomplete="new-password" />
          <span v-if="confirmError" class="field-error">{{ confirmError }}</span>
        </div>

        <div v-if="errorMsg" class="error-message" role="alert">{{ errorMsg }}</div>
        <div v-if="successMsg" class="success-message" role="status">{{ successMsg }}</div>

        <button type="submit" class="submit-btn" :disabled="loading || !isValid">
          {{ loading ? '提交中...' : '确认修改' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { changePassword } from '@/api/admin'

const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const newPwdError = ref('')
const confirmError = ref('')

function validateNew() {
  const p = form.newPassword
  if (!p) { newPwdError.value = ''; return }
  if (p.length < 8) { newPwdError.value = '密码长度不能少于 8 位'; return }
  if (!/[a-zA-Z]/.test(p) || !/\d/.test(p)) { newPwdError.value = '密码必须同时包含字母和数字'; return }
  newPwdError.value = ''
}

const strength = computed(() => {
  const p = form.newPassword
  if (!p) return 0
  let s = 0
  if (p.length >= 8) s++
  if (p.length >= 12) s++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^a-zA-Z0-9]/.test(p)) s++
  return s
})
const strengthClass = computed(() => strength.value <= 1 ? 'weak' : strength.value <= 3 ? 'medium' : 'strong')
const strengthWidth = computed(() => `${Math.min(100, strength.value * 20)}%`)

const isValid = computed(() => {
  const { currentPassword, newPassword, confirmPassword } = form
  return currentPassword && !newPwdError.value && newPassword.length >= 8 &&
    /[a-zA-Z]/.test(newPassword) && /\d/.test(newPassword) &&
    newPassword === confirmPassword
})

async function handleSubmit() {
  confirmError.value = form.newPassword !== form.confirmPassword ? '两次密码不一致' : ''
  if (confirmError.value || !isValid.value) return

  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
    successMsg.value = '密码修改成功，请重新登录'
    Object.assign(form, { currentPassword: '', newPassword: '', confirmPassword: '' })
  } catch (e) {
    const code = e?.response?.data?.code
    errorMsg.value = code === 'CURRENT_PASSWORD_WRONG' ? '当前密码错误' :
      code === 'WEAK_PASSWORD' ? '新密码强度不足' : '修改失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { display: flex; justify-content: center; padding-top: 20px; }
.card { background: #fff; border-radius: 8px; padding: 36px; width: 100%; max-width: 440px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.card h2 { margin: 0 0 24px; font-size: 18px; color: #1a3a6b; }
form { display: flex; flex-direction: column; gap: 18px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 14px; font-weight: 500; color: #333; }
.form-group input { height: 40px; padding: 0 12px; border: 1px solid #d0d7de; border-radius: 6px; font-size: 14px; outline: none; transition: border-color 0.2s; }
.form-group input:focus { border-color: #2d6a9f; box-shadow: 0 0 0 3px rgba(45,106,159,0.15); }
.field-error { font-size: 12px; color: #cf1322; }
.strength-bar { height: 4px; background: #e8e8e8; border-radius: 2px; overflow: hidden; }
.strength-fill { height: 100%; border-radius: 2px; transition: width 0.3s, background 0.3s; }
.strength-fill.weak { background: #ff4d4f; }
.strength-fill.medium { background: #faad14; }
.strength-fill.strong { background: #52c41a; }
.error-message { background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; padding: 10px 12px; font-size: 13px; color: #cf1322; }
.success-message { background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 6px; padding: 10px 12px; font-size: 13px; color: #389e0d; }
.submit-btn { height: 42px; background: #1a3a6b; color: #fff; border: none; border-radius: 6px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.2s, opacity 0.2s; }
.submit-btn:hover:not(:disabled) { background: #2d6a9f; }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
