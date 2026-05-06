<template>
  <div class="forbidden-page">
    <div class="forbidden-card">
      <div class="error-code">403</div>
      <div class="error-icon">🚫</div>
      <h2>访问受限</h2>
      <p>您没有权限访问该页面。如需访问，请联系系统管理员申请相应权限。</p>
      <div class="actions">
        <router-link to="/dashboard" class="btn-home">返回首页</router-link>
        <button class="btn-back" @click="$router.back()">返回上页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: #f0f2f5;
}
.forbidden-card {
  background: #fff; border-radius: 16px; padding: 60px 48px;
  text-align: center; box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  max-width: 440px; width: 100%;
}
.error-code {
  font-size: 80px; font-weight: 900; color: #e8e8e8;
  line-height: 1; margin-bottom: 8px; letter-spacing: -4px;
}
.error-icon { font-size: 48px; margin-bottom: 16px; }
h2 { margin: 0 0 12px; font-size: 22px; color: #1a3a6b; }
p { margin: 0 0 28px; font-size: 14px; color: #888; line-height: 1.7; }
.actions { display: flex; gap: 12px; justify-content: center; }
.btn-home {
  background: #1a3a6b; color: #fff; text-decoration: none;
  border-radius: 8px; padding: 10px 24px; font-size: 14px;
  transition: background 0.15s;
}
.btn-home:hover { background: #2d6a9f; }
.btn-back {
  background: #fff; border: 1px solid #d0d7de; border-radius: 8px;
  padding: 10px 24px; font-size: 14px; cursor: pointer; color: #555;
}
.btn-back:hover { background: #f5f5f5; }
</style>
