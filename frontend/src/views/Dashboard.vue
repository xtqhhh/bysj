<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="banner">
      <div class="banner-left">
        <div class="banner-avatar">{{ avatarChar }}</div>
        <div>
          <h2>欢迎回来，{{ userInfo.realName || userInfo.username || '用户' }}</h2>
          <p>{{ today }} · <span v-if="!isCitizen" :class="['role-inline', roleClass]">{{ roleLabel }}</span><span v-if="!isCitizen"> · </span>账号状态正常</p>
        </div>
      </div>
      <div class="banner-right">
        <div class="banner-badge">🏛 政务安全认证平台</div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="s in statCards" :key="s.label" :style="{ '--accent': s.color }">
        <div class="stat-icon-wrap">{{ s.icon }}</div>
        <div class="stat-body">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
        <div class="stat-bg">{{ s.icon }}</div>
      </div>
    </div>

    <!-- 群众用户视图 -->
    <template v-if="isCitizen">
      <!-- 快捷申请入口 -->
      <div class="section-card">
        <div class="card-header">
          <span class="card-title">📋 政务服务申请</span>
          <button class="btn-primary-sm" @click="showApplyModal = true">+ 新建申请</button>
        </div>
        <div class="service-quick-grid">
          <div class="service-quick-item" v-for="s in serviceTypes" :key="s.type"
            @click="openApplyWithType(s)">
            <div class="sq-icon" :style="{ background: s.color }">{{ s.icon }}</div>
            <div class="sq-name">{{ s.name }}</div>
            <div class="sq-time">{{ s.time }}</div>
          </div>
        </div>
      </div>

      <!-- 我的申请列表 -->
      <div class="section-card">
        <div class="card-header">
          <span class="card-title">📂 我的申请</span>
          <span class="card-sub">共 {{ appTotal }} 条</span>
        </div>
        <div v-if="appLoading" class="empty-tip">加载中...</div>
        <div v-else-if="!appList.length" class="empty-tip">
          暂无申请记录，点击上方服务项目快速发起申请
        </div>
        <div v-else class="app-list">
          <div class="app-item" v-for="app in appList" :key="app.id" @click="viewDetail(app)">
            <div class="app-item-left">
              <div class="app-type-tag">{{ app.type }}</div>
              <div class="app-title">{{ app.title }}</div>
              <div class="app-meta">
                <span>申请编号：{{ app.applyNo }}</span>
                <span>{{ formatTime(app.createTime) }}</span>
              </div>
            </div>
            <div class="app-item-right">
              <div :class="['app-status', statusClass(app.status)]">
                {{ statusLabel(app.status) }}
              </div>
              <div class="app-progress">
                <div class="progress-steps">
                  <div v-for="(step, i) in progressSteps" :key="i"
                    :class="['step', { done: app.status > i && app.status < 5, active: app.status === i }]">
                    <div class="step-dot"></div>
                    <div class="step-label">{{ step }}</div>
                  </div>
                </div>
              </div>
              <button v-if="app.status === 0" class="cancel-btn"
                @click.stop="handleCancel(app)">撤销申请</button>
            </div>
          </div>
        </div>
        <div v-if="appTotal > appPageSize" class="list-pagination">
          <button :disabled="appPage <= 1" @click="loadApps(appPage - 1)">上一页</button>
          <span>{{ appPage }} / {{ Math.ceil(appTotal / appPageSize) }}</span>
          <button :disabled="appPage >= Math.ceil(appTotal / appPageSize)" @click="loadApps(appPage + 1)">下一页</button>
        </div>
      </div>

      <!-- 账号信息 + 我的留言 -->
      <div class="info-row">
        <div class="info-card">
          <div class="card-header">
            <span class="card-title">账号信息</span>
            <router-link to="/app/profile/password" class="card-action">修改密码 →</router-link>
          </div>
          <div class="info-grid">
            <div class="info-item"><span class="info-label">用户名</span><span class="info-value">{{ userInfo.username }}</span></div>
            <div class="info-item"><span class="info-label">真实姓名</span><span class="info-value">{{ userInfo.realName || '-' }}</span></div>
          </div>
        </div>
        <div class="info-card">
          <div class="card-header">
            <span class="card-title">我的留言</span>
            <router-link to="/" class="card-action">去留言 →</router-link>
          </div>
          <div v-if="!myMessages.length" class="empty-tip">暂无留言记录</div>
          <div v-else class="my-msg-list">
            <div class="my-msg-item" v-for="m in myMessages" :key="m.id">
              <div class="my-msg-header">
                <span :class="['audit-tag', auditClass(m.auditStatus)]">{{ auditLabel(m.auditStatus) }}</span>
                <span class="my-msg-time">{{ formatTime(m.createTime) }}</span>
              </div>
              <div class="my-msg-content">{{ m.content }}</div>
              <div v-if="m.reply" class="my-msg-reply"><span class="reply-label">官方回复：</span>{{ m.reply }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="security-tips">
        <div class="tips-header">🔐 安全提示</div>
        <div class="tips-list">
          <div class="tip-item" v-for="tip in securityTips" :key="tip"><span class="tip-dot">·</span>{{ tip }}</div>
        </div>
      </div>
    </template>

    <!-- 工作人员/超管视图 -->
    <template v-else>
      <div class="info-row">
        <div class="info-card">
          <div class="card-header">
            <span class="card-title">账号信息</span>
            <router-link to="/app/profile/password" class="card-action">修改密码 →</router-link>
          </div>
          <div class="info-grid">
            <div class="info-item"><span class="info-label">用户名</span><span class="info-value">{{ userInfo.username }}</span></div>
            <div class="info-item"><span class="info-label">真实姓名</span><span class="info-value">{{ userInfo.realName || '-' }}</span></div>
            <div class="info-item"><span class="info-label">当前角色</span><span class="info-value"><span :class="['role-badge', roleClass]">{{ roleLabel }}</span></span></div>
            <div class="info-item"><span class="info-label">数据权限</span><span class="info-value">{{ dataScopeLabel }}</span></div>
            <div class="info-item"><span class="info-label">已授权功能</span><span class="info-value">{{ permissions.length }} 项</span></div>
          </div>
        </div>
        <div class="info-card">
          <div class="card-header"><span class="card-title">已授权功能</span></div>
          <div v-if="permissions.length" class="perm-list">
            <span v-for="p in permissions" :key="p" class="perm-tag">{{ p }}</span>
          </div>
          <div v-else class="empty-tip">暂无特殊权限</div>
        </div>
      </div>
      <div class="mgmt-section">
        <div class="card-header" style="margin-bottom:16px"><span class="card-title">系统管理</span></div>
        <div class="mgmt-grid">
          <router-link v-for="item in mgmtItems" :key="item.path" :to="item.path" class="mgmt-card">
            <div class="mgmt-icon" :style="{ background: item.color }">{{ item.icon }}</div>
            <div class="mgmt-name">{{ item.name }}</div>
            <div class="mgmt-desc">{{ item.desc }}</div>
            <div class="mgmt-arrow">→</div>
          </router-link>
        </div>
      </div>
      <div class="security-tips">
        <div class="tips-header">🔐 安全提示</div>
        <div class="tips-list">
          <div class="tip-item" v-for="tip in securityTips" :key="tip"><span class="tip-dot">·</span>{{ tip }}</div>
        </div>
      </div>
    </template>

    <!-- 新建申请弹窗 -->
    <div v-if="showApplyModal" class="modal-mask" @click.self="showApplyModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>发起政务申请</h3>
          <button class="modal-close" @click="showApplyModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>申请类型 <span class="required">*</span></label>
            <select v-model="applyForm.type">
              <option v-for="s in serviceTypes" :key="s.type" :value="s.type">{{ s.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>申请标题 <span class="required">*</span></label>
            <input v-model="applyForm.title" placeholder="请简要描述申请事项" />
          </div>
          <div class="form-group">
            <label>详细描述</label>
            <textarea v-model="applyForm.description" rows="4" placeholder="请详细描述申请内容、原因及相关情况..."></textarea>
          </div>
          <div v-if="applyError" class="form-error">{{ applyError }}</div>
          <div v-if="applySuccess" class="form-success">
            ✅ 申请提交成功！申请编号：<b>{{ applySuccess }}</b>，请保存以便查询进度。
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="showApplyModal = false">关闭</button>
          <button v-if="!applySuccess" class="btn-primary" @click="submitApply" :disabled="applySubmitting">
            {{ applySubmitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 申请详情弹窗 -->
    <div v-if="detailApp" class="modal-mask" @click.self="detailApp = null">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h3>申请详情</h3>
          <button class="modal-close" @click="detailApp = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">申请编号</span><span class="detail-value mono">{{ detailApp.applyNo }}</span></div>
            <div class="detail-item"><span class="detail-label">申请类型</span><span class="detail-value">{{ detailApp.type }}</span></div>
            <div class="detail-item"><span class="detail-label">申请时间</span><span class="detail-value">{{ formatTime(detailApp.createTime) }}</span></div>
            <div class="detail-item">
              <span class="detail-label">当前状态</span>
              <span :class="['app-status', statusClass(detailApp.status)]">{{ statusLabel(detailApp.status) }}</span>
            </div>
          </div>
          <div class="detail-full">
            <span class="detail-label">申请标题</span>
            <div class="detail-content">{{ detailApp.title }}</div>
          </div>
          <div v-if="detailApp.description" class="detail-full">
            <span class="detail-label">详细描述</span>
            <div class="detail-content">{{ detailApp.description }}</div>
          </div>
          <!-- 进度时间线 -->
          <div class="timeline">
            <div class="timeline-title">办理进度</div>
            <div class="timeline-steps">
              <div v-for="(step, i) in progressSteps" :key="i"
                :class="['tl-step', { done: detailApp.status > i, active: detailApp.status === i, pending: detailApp.status < i }]">
                <div class="tl-dot">{{ detailApp.status > i ? '✓' : i + 1 }}</div>
                <div class="tl-info">
                  <div class="tl-name">{{ step }}</div>
                  <div class="tl-desc">{{ stepDesc[i] }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="detailApp.remark" class="remark-box">
            <span class="remark-label">审核备注：</span>{{ detailApp.remark }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="detailApp = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyMessages } from '@/api/auth'
import { submitApplication, getMyApplications, cancelApplication } from '@/api/auth'

const userInfo = computed(() => { try { return JSON.parse(localStorage.getItem('user_info') || '{}') } catch { return {} } })
const permissions = computed(() => { try { return JSON.parse(localStorage.getItem('permissions') || '[]') } catch { return [] } })
const roles = computed(() => userInfo.value.roles || [])
const isCitizen = computed(() => !roles.value.includes('ROLE_SUPER_ADMIN') && !roles.value.includes('ROLE_OFFICER'))
const isAdmin = computed(() => roles.value.includes('ROLE_SUPER_ADMIN'))
const avatarChar = computed(() => (userInfo.value.realName || userInfo.value.username || '?').charAt(0).toUpperCase())
const roleLabel = computed(() => { if (roles.value.includes('ROLE_SUPER_ADMIN')) return '超级管理员'; if (roles.value.includes('ROLE_OFFICER')) return '工作人员'; return '群众用户' })
const roleClass = computed(() => { if (roles.value.includes('ROLE_SUPER_ADMIN')) return 'admin'; if (roles.value.includes('ROLE_OFFICER')) return 'officer'; return 'citizen' })
const dataScopeLabel = computed(() => { if (roles.value.includes('ROLE_SUPER_ADMIN')) return '全部数据'; if (roles.value.includes('ROLE_OFFICER')) return '本部门数据'; return '本人数据' })
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

// ── 申请状态 ──
const progressSteps = ['待受理', '审核中', '已通过', '已完成']
const stepDesc = ['申请已提交，等待工作人员受理', '工作人员正在审核您的申请材料', '申请已审核通过，正在办理', '申请已办理完成']
const STATUS_MAP = {
  0: { label: '待受理', cls: 'status-pending' },
  1: { label: '审核中', cls: 'status-processing' },
  2: { label: '已通过', cls: 'status-approved' },
  3: { label: '已拒绝', cls: 'status-rejected' },
  4: { label: '已完成', cls: 'status-done' },
  5: { label: '已撤销', cls: 'status-cancelled' },
}
const statusLabel = s => STATUS_MAP[s]?.label || s
const statusClass = s => STATUS_MAP[s]?.cls || ''

// ── 服务类型 ──
const serviceTypes = [
  { type: '身份证办理', name: '身份证办理', icon: '🪪', color: '#e6f4ff', time: '5个工作日' },
  { type: '户籍迁移', name: '户籍迁移', icon: '🏠', color: '#f6ffed', time: '10个工作日' },
  { type: '婚姻登记', name: '婚姻登记', icon: '💍', color: '#fff7e6', time: '当日办结' },
  { type: '出生登记', name: '出生登记', icon: '👶', color: '#f9f0ff', time: '3个工作日' },
  { type: '医保办理', name: '医保办理', icon: '🏥', color: '#fff2f0', time: '5个工作日' },
  { type: '学历认证', name: '学历认证', icon: '🎓', color: '#e6fffb', time: '15个工作日' },
  { type: '驾照申请', name: '驾照申请', icon: '🚗', color: '#feffe6', time: '20个工作日' },
  { type: '其他事项', name: '其他事项', icon: '📋', color: '#f5f5f5', time: '视情况而定' },
]

// ── 我的申请 ──
const appList = ref([])
const appTotal = ref(0)
const appPage = ref(1)
const appPageSize = 5
const appLoading = ref(false)
const detailApp = ref(null)

async function loadApps(page = 1) {
  appLoading.value = true
  appPage.value = page
  try {
    const res = await getMyApplications({ page, size: appPageSize })
    appList.value = res.data.data?.list || []
    appTotal.value = res.data.data?.total || 0
  } catch { appList.value = [] } finally { appLoading.value = false }
}

function viewDetail(app) { detailApp.value = app }

async function handleCancel(app) {
  if (!confirm(`确认撤销申请「${app.title}」？撤销后不可恢复。`)) return
  try {
    await cancelApplication(app.id)
    app.status = 5
  } catch (e) {
    alert(e?.response?.data?.message || '撤销失败，请稍后重试')
  }
}

// ── 新建申请 ──
const showApplyModal = ref(false)
const applyForm = ref({ type: '身份证办理', title: '', description: '' })
const applySubmitting = ref(false)
const applyError = ref('')
const applySuccess = ref('')

function openApplyWithType(s) {
  applyForm.value = { type: s.type, title: '', description: '' }
  applyError.value = ''
  applySuccess.value = ''
  showApplyModal.value = true
}

async function submitApply() {
  if (!applyForm.value.title.trim()) { applyError.value = '请填写申请标题'; return }
  applySubmitting.value = true
  applyError.value = ''
  try {
    const res = await submitApplication(applyForm.value)
    applySuccess.value = res.data.data?.applyNo || '提交成功'
    applyForm.value = { type: '身份证办理', title: '', description: '' }
    loadApps(1)
  } catch (e) {
    applyError.value = e?.response?.data?.message || '提交失败，请稍后重试'
  } finally { applySubmitting.value = false }
}

// ── 我的留言 ──
const myMessages = ref([])
const AUDIT_MAP = { 0: { label: '待审核', cls: 'pending' }, 1: { label: '已通过', cls: 'approved' }, 2: { label: '已拒绝', cls: 'rejected' } }
const auditLabel = s => AUDIT_MAP[s]?.label || s
const auditClass = s => AUDIT_MAP[s]?.cls || ''

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// ── 统计卡片 ──
const statCards = computed(() => {
  if (isCitizen.value) return [
    { icon: '📋', label: '我的申请', value: `${appTotal.value} 条`, color: '#1a3a6b' },
    { icon: '⏳', label: '待处理', value: `${appList.value.filter(a => a.status <= 1).length} 条`, color: '#d46b08' },
    { icon: '✅', label: '已完成', value: `${appList.value.filter(a => a.status === 4).length} 条`, color: '#389e0d' },
    { icon: '💬', label: '我的留言', value: `${myMessages.value.length} 条`, color: '#2d6a9f' },
  ]
  return [
    { icon: '🛡', label: '当前角色', value: roleLabel.value, color: '#1a3a6b' },
    { icon: '✅', label: '账号状态', value: '正常', color: '#389e0d' },
    { icon: '🔑', label: '已授权功能', value: `${permissions.value.length} 项`, color: '#2d6a9f' },
    { icon: '📊', label: '数据权限', value: dataScopeLabel.value, color: '#c8a84b' },
  ]
})

const mgmtItems = computed(() => {
  const items = [{ path: '/app/admin/users', name: '用户管理', desc: '查询、创建、禁用用户账号', icon: '👤', color: '#e6f4ff' }]
  if (!isAdmin.value) items.push(
    { path: '/app/admin/applications', name: '申请管理', desc: '受理、审核群众政务申请', icon: '📋', color: '#fff7e6' },
  )
  if (isAdmin.value) items.push(
    { path: '/app/admin/roles', name: '角色管理', desc: '管理角色与权限分配', icon: '🛡', color: '#f6ffed' },
    { path: '/app/admin/audit-logs', name: '审计日志', desc: '查看系统操作日志记录', icon: '📋', color: '#fff7e6' },
    { path: '/app/admin/messages', name: '留言管理', desc: '审核、回复用户留言', icon: '💬', color: '#f9f0ff' },
    { path: '/app/admin/applications', name: '申请管理', desc: '受理、审核群众政务申请', icon: '📝', color: '#feffe6' },
  )
  return items
})

const securityTips = ['请勿将账号密码告知他人，定期更换密码可有效保障账号安全', '如发现账号异常登录，请立即修改密码并联系系统管理员', '本系统所有操作均有审计日志记录，请合规使用系统功能', '离开工作站时请及时退出登录，防止他人未授权访问']

onMounted(async () => {
  if (isCitizen.value) {
    loadApps()
    try { const res = await getMyMessages({ page: 1, size: 3 }); myMessages.value = res.data.data?.list || [] } catch { }
  }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 20px; }

.banner { background: linear-gradient(135deg, #0f2a52 0%, #1a3a6b 50%, #2d6a9f 100%); border-radius: 12px; padding: 28px 32px; display: flex; align-items: center; justify-content: space-between; color: #fff; box-shadow: 0 4px 16px rgba(26,58,107,0.3); }
.banner-left { display: flex; align-items: center; gap: 18px; }
.banner-avatar { width: 56px; height: 56px; border-radius: 50%; background: rgba(200,168,75,0.3); border: 2px solid rgba(200,168,75,0.6); font-size: 22px; font-weight: 700; color: #e8c96a; display: flex; align-items: center; justify-content: center; }
.banner-left h2 { margin: 0 0 6px; font-size: 20px; font-weight: 700; }
.banner-left p { margin: 0; font-size: 13px; opacity: 0.75; }
.banner-badge { background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 20px; padding: 8px 16px; font-size: 13px; }
.role-inline { padding: 1px 8px; border-radius: 8px; font-size: 12px; }
.role-inline.admin { background: rgba(200,168,75,0.3); color: #e8c96a; }
.role-inline.officer { background: rgba(45,106,159,0.3); color: #90c8f0; }
.role-inline.citizen { background: rgba(82,196,26,0.2); color: #95de64; }

.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { background: #fff; border-radius: 10px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); position: relative; overflow: hidden; border-top: 3px solid var(--accent); }
.stat-icon-wrap { font-size: 28px; flex-shrink: 0; }
.stat-value { font-size: 20px; font-weight: 700; color: var(--accent); line-height: 1.2; }
.stat-label { font-size: 12px; color: #888; margin-top: 3px; }
.stat-bg { position: absolute; right: -8px; bottom: -8px; font-size: 56px; opacity: 0.05; pointer-events: none; }

.section-card { background: #fff; border-radius: 10px; padding: 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-title { font-size: 14px; font-weight: 600; color: #1a3a6b; }
.card-sub { font-size: 12px; color: #aaa; }
.card-action { font-size: 12px; color: #2d6a9f; text-decoration: none; }
.card-action:hover { text-decoration: underline; }
.btn-primary-sm { background: #1a3a6b; color: #fff; border: none; border-radius: 6px; padding: 5px 14px; font-size: 13px; cursor: pointer; }
.btn-primary-sm:hover { background: #2d6a9f; }

/* 快捷服务 */
.service-quick-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 12px; }
@media (max-width: 1000px) { .service-quick-grid { grid-template-columns: repeat(4, 1fr); } }
.service-quick-item { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 14px 8px; border-radius: 10px; cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.service-quick-item:hover { border-color: #c5d5f0; box-shadow: 0 2px 8px rgba(26,58,107,0.1); transform: translateY(-2px); }
.sq-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.sq-name { font-size: 12px; font-weight: 600; color: #333; text-align: center; }
.sq-time { font-size: 11px; color: #aaa; text-align: center; }

/* 申请列表 */
.app-list { display: flex; flex-direction: column; gap: 10px; }
.app-item { display: flex; align-items: center; justify-content: space-between; background: #f8f9fb; border-radius: 10px; padding: 14px 16px; cursor: pointer; transition: all 0.15s; border: 1px solid transparent; }
.app-item:hover { background: #fff; border-color: #c5d5f0; box-shadow: 0 2px 8px rgba(26,58,107,0.08); }
.app-type-tag { display: inline-block; background: #e6f4ff; color: #0958d9; padding: 2px 8px; border-radius: 8px; font-size: 11px; margin-bottom: 5px; }
.app-title { font-size: 14px; font-weight: 500; color: #333; margin-bottom: 4px; }
.app-meta { font-size: 11px; color: #aaa; display: flex; gap: 12px; }
.app-item-right { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.app-status { padding: 3px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-pending { background: #fff7e6; color: #d46b08; }
.status-processing { background: #e6f4ff; color: #0958d9; }
.status-approved { background: #f0fff4; color: #22863a; }
.status-rejected { background: #fff0f0; color: #cb2431; }
.status-done { background: #f6ffed; color: #389e0d; }
.status-cancelled { background: #f5f5f5; color: #999; }

.cancel-btn {
  background: none; border: 1px solid #ffccc7; border-radius: 6px;
  padding: 3px 10px; font-size: 12px; cursor: pointer; color: #cf1322;
  transition: all 0.15s;
}
.cancel-btn:hover { background: #fff2f0; }

.progress-steps { display: flex; gap: 4px; align-items: center; }
.step { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.step-dot { width: 8px; height: 8px; border-radius: 50%; background: #e0e0e0; }
.step.done .step-dot { background: #389e0d; }
.step.active .step-dot { background: #0958d9; box-shadow: 0 0 0 3px rgba(9,88,217,0.2); }
.step-label { font-size: 10px; color: #aaa; white-space: nowrap; }
.step.done .step-label { color: #389e0d; }
.step.active .step-label { color: #0958d9; font-weight: 600; }

.list-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 12px; font-size: 13px; color: #555; }
.list-pagination button { padding: 4px 14px; border: 1px solid #d0d7de; border-radius: 6px; cursor: pointer; background: #fff; }
.list-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

/* 账号信息 */
.info-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-card { background: #fff; border-radius: 10px; padding: 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item { display: flex; flex-direction: column; gap: 5px; }
.info-label { font-size: 11px; color: #aaa; text-transform: uppercase; letter-spacing: 0.5px; }
.info-value { font-size: 14px; color: #333; font-weight: 500; }
.role-badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.role-badge.admin { background: #fff3cd; color: #856404; }
.role-badge.officer { background: #d1ecf1; color: #0c5460; }
.role-badge.citizen { background: #d4edda; color: #155724; }
.perm-list { display: flex; flex-wrap: wrap; gap: 8px; }
.perm-tag { background: #f0f4ff; color: #1a3a6b; border: 1px solid #c5d5f0; padding: 3px 10px; border-radius: 12px; font-size: 12px; }
.empty-tip { color: #aaa; font-size: 13px; padding: 12px 0; }

/* 留言 */
.my-msg-list { display: flex; flex-direction: column; gap: 10px; }
.my-msg-item { background: #f8f9fb; border-radius: 8px; padding: 12px; }
.my-msg-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.audit-tag { padding: 2px 8px; border-radius: 8px; font-size: 11px; font-weight: 500; }
.audit-tag.pending { background: #fff7e6; color: #d46b08; }
.audit-tag.approved { background: #f0fff4; color: #22863a; }
.audit-tag.rejected { background: #fff0f0; color: #cb2431; }
.my-msg-time { font-size: 11px; color: #aaa; }
.my-msg-content { font-size: 13px; color: #333; line-height: 1.6; }
.my-msg-reply { margin-top: 8px; background: #f0f4ff; border-left: 3px solid #1a3a6b; padding: 6px 10px; border-radius: 0 6px 6px 0; font-size: 12px; color: #1a3a6b; }
.reply-label { font-weight: 600; margin-right: 4px; }

/* 管理入口 */
.mgmt-section { background: #fff; border-radius: 10px; padding: 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.mgmt-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.mgmt-card { display: flex; flex-direction: column; align-items: center; gap: 8px; background: #f8f9fb; border-radius: 10px; padding: 20px 16px; text-decoration: none; transition: all 0.2s; border: 1px solid transparent; position: relative; }
.mgmt-card:hover { background: #fff; border-color: #c5d5f0; box-shadow: 0 4px 16px rgba(26,58,107,0.1); transform: translateY(-2px); }
.mgmt-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.mgmt-name { font-size: 14px; font-weight: 600; color: #1a3a6b; }
.mgmt-desc { font-size: 12px; color: #888; text-align: center; line-height: 1.5; }
.mgmt-arrow { position: absolute; top: 12px; right: 14px; font-size: 14px; color: #ccc; }
.mgmt-card:hover .mgmt-arrow { color: #2d6a9f; }

/* 安全提示 */
.security-tips { background: #fffbe6; border: 1px solid #ffe58f; border-radius: 10px; padding: 16px 20px; }
.tips-header { font-size: 13px; font-weight: 600; color: #856404; margin-bottom: 10px; }
.tips-list { display: flex; flex-direction: column; gap: 6px; }
.tip-item { font-size: 13px; color: #7a5c00; display: flex; gap: 8px; }
.tip-dot { color: #c8a84b; font-weight: 700; }

/* 弹窗 */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 12px; width: 520px; box-shadow: 0 8px 32px rgba(0,0,0,0.2); max-height: 85vh; overflow-y: auto; }
.modal-wide { width: 600px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px 0; position: sticky; top: 0; background: #fff; }
.modal-header h3 { margin: 0; font-size: 16px; color: #1a3a6b; }
.modal-close { background: none; border: none; font-size: 16px; cursor: pointer; color: #aaa; }
.modal-body { padding: 16px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 0 24px 20px; position: sticky; bottom: 0; background: #fff; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: 13px; color: #555; font-weight: 500; }
.form-group input, .form-group select { height: 36px; padding: 0 10px; border: 1px solid #d0d7de; border-radius: 7px; font-size: 13px; outline: none; }
.form-group input:focus, .form-group select:focus { border-color: #2d6a9f; }
.form-group textarea { padding: 10px; border: 1px solid #d0d7de; border-radius: 7px; font-size: 13px; outline: none; resize: vertical; font-family: inherit; }
.form-group textarea:focus { border-color: #2d6a9f; }
.required { color: #cf1322; }
.form-error { background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; padding: 8px 12px; font-size: 13px; color: #cf1322; margin-bottom: 8px; }
.form-success { background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 6px; padding: 10px 14px; font-size: 13px; color: #389e0d; }
.btn-primary { background: #1a3a6b; color: #fff; border: none; border-radius: 7px; padding: 7px 20px; font-size: 13px; cursor: pointer; }
.btn-primary:hover:not(:disabled) { background: #2d6a9f; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 7px; padding: 6px 18px; font-size: 13px; cursor: pointer; }

/* 详情 */
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 11px; color: #aaa; text-transform: uppercase; letter-spacing: 0.5px; }
.detail-value { font-size: 14px; color: #333; font-weight: 500; }
.detail-value.mono { font-family: monospace; font-size: 13px; }
.detail-full { margin-bottom: 14px; }
.detail-content { background: #f8f9fb; border-radius: 8px; padding: 12px; font-size: 14px; color: #333; line-height: 1.7; margin-top: 6px; }

/* 时间线 */
.timeline { margin: 16px 0; }
.timeline-title { font-size: 13px; font-weight: 600; color: #555; margin-bottom: 16px; }
.timeline-steps { display: flex; gap: 0; }
.tl-step { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.tl-step:not(:last-child)::after { content: ''; position: absolute; top: 16px; left: 50%; width: 100%; height: 2px; background: #e0e0e0; z-index: 0; }
.tl-step.done:not(:last-child)::after { background: #389e0d; }
.tl-dot { width: 32px; height: 32px; border-radius: 50%; background: #e0e0e0; color: #aaa; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; position: relative; z-index: 1; }
.tl-step.done .tl-dot { background: #389e0d; color: #fff; }
.tl-step.active .tl-dot { background: #0958d9; color: #fff; box-shadow: 0 0 0 4px rgba(9,88,217,0.15); }
.tl-info { text-align: center; margin-top: 8px; }
.tl-name { font-size: 12px; font-weight: 600; color: #333; }
.tl-step.done .tl-name { color: #389e0d; }
.tl-step.active .tl-name { color: #0958d9; }
.tl-step.pending .tl-name { color: #aaa; }
.tl-desc { font-size: 11px; color: #aaa; margin-top: 3px; max-width: 100px; line-height: 1.4; }
.remark-box { background: #fff7e6; border: 1px solid #ffe58f; border-radius: 8px; padding: 10px 14px; font-size: 13px; color: #7a5c00; }
.remark-label { font-weight: 600; margin-right: 4px; }
</style>
