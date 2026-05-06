<template>
  <div class="portal">
    <!-- 顶部导航 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="nav-brand">
          <div class="nav-emblem">国</div>
          <div>
            <div class="nav-title">政务服务平台</div>
            <div class="nav-sub">Gov Service Platform</div>
          </div>
        </div>
        <nav class="nav-links">
          <a href="#services">服务大厅</a>
          <a href="#news">政务动态</a>
          <a href="#notice">通知公告</a>
          <a href="#query">办事查询</a>
        </nav>
        <div v-if="!isLoggedIn" class="nav-actions">
          <router-link to="/login" class="btn-login">登录</router-link>
          <router-link to="/register" class="btn-register">注册</router-link>
        </div>
        <div v-else class="nav-user">
          <div class="nav-avatar">{{ avatarChar }}</div>
          <div class="nav-user-info">
            <span class="nav-username">{{ userInfo.realName || userInfo.username }}</span>
            <span class="nav-role">{{ roleLabel }}</span>
          </div>
          <router-link to="/app/dashboard" class="btn-register">进入个人中心</router-link>
          <button class="btn-logout" @click="handleLogout">退出</button>
        </div>
      </div>
    </header>

    <!-- 轮播 Banner -->
    <section class="banner">
      <div class="banner-track" :style="{ transform: `translateX(-${bannerIndex * 100}%)` }">
        <div class="banner-slide" v-for="(slide, i) in bannerSlides" :key="i" :style="{ background: slide.bg }">
          <div class="banner-content">
            <div class="banner-tag">{{ slide.tag }}</div>
            <h2>{{ slide.title }}</h2>
            <p>{{ slide.desc }}</p>
            <template v-if="!isLoggedIn">
              <router-link to="/register" class="banner-btn">立即办理</router-link>
            </template>
            <template v-else>
              <router-link to="/app/dashboard" class="banner-btn">进入个人中心</router-link>
            </template>
          </div>
          <div class="banner-deco">{{ slide.icon }}</div>
        </div>
      </div>
      <div class="banner-dots">
        <span v-for="(_, i) in bannerSlides" :key="i"
          :class="['dot', { active: i === bannerIndex }]"
          @click="bannerIndex = i" />
      </div>
      <button class="banner-arrow left" @click="prevBanner">‹</button>
      <button class="banner-arrow right" @click="nextBanner">›</button>
    </section>

    <!-- 快捷入口 -->
    <section class="quick-bar">
      <div class="quick-inner">
        <div class="quick-item" v-for="q in quickLinks" :key="q.label">
          <span class="quick-icon">{{ q.icon }}</span>
          <span>{{ q.label }}</span>
        </div>
      </div>
    </section>

    <!-- 办事进度查询 -->
    <section class="query-section" id="query">
      <div class="section-inner">
        <div class="query-card">
          <div class="query-left">
            <h3>📋 办事进度查询</h3>
            <p>输入申请编号，实时查询您的政务事项办理进度</p>
          </div>
          <div class="query-right">
            <div class="query-input-group">
              <select v-model="queryType">
                <option value="apply">申请编号</option>
                <option value="id">身份证号</option>
                <option value="phone">手机号</option>
              </select>
              <input v-model="queryCode" :placeholder="queryPlaceholder" @keyup.enter="doQuery" />
              <button @click="doQuery">查 询</button>
            </div>
            <div v-if="queryResult" class="query-result" :class="queryResult.status">
              <span class="result-icon">{{ queryResult.icon }}</span>
              <span>{{ queryResult.text }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 服务大厅 -->
    <section class="services" id="services">
      <div class="section-inner">
        <div class="section-header">
          <h2>服务大厅</h2>
          <p>一站式政务服务，足不出户办理各类事项</p>
        </div>
        <div class="service-tabs">
          <button v-for="tab in serviceTabs" :key="tab.key"
            :class="['tab-btn', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key">
            {{ tab.icon }} {{ tab.label }}
          </button>
        </div>
        <div class="service-grid">
          <div class="service-card" v-for="s in currentServices" :key="s.title">
            <div class="service-icon-wrap" :style="{ background: s.color }">{{ s.icon }}</div>
            <div class="service-body">
              <div class="service-title">{{ s.title }}</div>
              <div class="service-desc">{{ s.desc }}</div>
              <div class="service-meta">
                <span class="service-time">⏱ {{ s.time }}</span>
                <span class="service-hot" v-if="s.hot">🔥 热门</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 政务动态 + 通知公告 -->
    <section class="news-section" id="news">
      <div class="section-inner">
        <div class="news-notice-grid">
          <!-- 政务动态 -->
          <div class="news-block">
            <div class="block-header">
              <h2>政务动态</h2>
              <a href="#" class="more-link">更多 →</a>
            </div>
            <div class="news-featured-card">
              <div class="featured-cover">
                <div class="cover-bg">{{ newsList[0].icon }}</div>
                <span class="featured-tag">头条</span>
              </div>
              <div class="featured-body">
                <h4>{{ newsList[0].title }}</h4>
                <p>{{ newsList[0].summary }}</p>
                <div class="news-meta-row">
                  <span>{{ newsList[0].source }}</span>
                  <span>{{ newsList[0].date }}</span>
                </div>
              </div>
            </div>
            <div class="news-sub-list">
              <div class="news-sub-item" v-for="item in newsList.slice(1)" :key="item.title">
                <span :class="['tag-dot', item.tagClass]"></span>
                <div class="sub-item-body">
                  <div class="sub-title">{{ item.title }}</div>
                  <div class="sub-meta">{{ item.source }} · {{ item.date }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 通知公告 + 政策法规 -->
          <div class="side-blocks">
            <div class="notice-block" id="notice">
              <div class="block-header">
                <h2>📢 通知公告</h2>
                <a href="#" class="more-link">更多 →</a>
              </div>
              <div class="notice-list">
                <div class="notice-item" v-for="n in notices" :key="n.text">
                  <span class="notice-dot"></span>
                  <div class="notice-body">
                    <div class="notice-text">{{ n.text }}</div>
                    <div class="notice-date">{{ n.date }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="policy-block">
              <div class="block-header">
                <h2>📜 政策法规</h2>
                <a href="#" class="more-link">更多 →</a>
              </div>
              <div class="policy-list">
                <div class="policy-item" v-for="p in policies" :key="p.title">
                  <div class="policy-date">{{ p.date }}</div>
                  <div class="policy-title">{{ p.title }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 留言板 -->
    <section class="message-section">
      <div class="section-inner">
        <div class="section-header">
          <h2>留言板</h2>
          <p>欢迎留下您的意见与建议，管理员审核通过后将公开展示</p>
        </div>
        <div class="message-layout">
          <!-- 提交表单 -->
          <div class="message-form-card">
            <div class="form-card-title">✍️ 我要留言</div>
            <template v-if="isLoggedIn">
              <div class="form-user-info">
                <div class="form-avatar">{{ avatarChar }}</div>
                <span>以 <b>{{ userInfo.realName || userInfo.username }}</b> 的身份留言</span>
              </div>
              <textarea
                v-model="msgContent"
                placeholder="请输入您的留言内容（最多500字）..."
                maxlength="500"
                rows="5"
              ></textarea>
              <div class="form-footer">
                <span class="char-count">{{ msgContent.length }} / 500</span>
                <button class="submit-msg-btn" :disabled="!msgContent.trim() || msgSubmitting" @click="submitMsg">
                  {{ msgSubmitting ? '提交中...' : '提交留言' }}
                </button>
              </div>
              <div v-if="msgSuccess" class="msg-tip success">✅ 留言提交成功，等待管理员审核后将公开显示</div>
              <div v-if="msgError" class="msg-tip error">⚠️ {{ msgError }}</div>
            </template>
            <template v-else>
              <div class="login-tip">
                <span>💬</span>
                <p>请先登录后再留言</p>
                <router-link to="/login" class="btn-register">立即登录</router-link>
              </div>
            </template>
          </div>

          <!-- 留言列表 -->
          <div class="message-list-card">
            <div class="list-header">
              <span>公开留言（{{ msgTotal }} 条）</span>
              <button class="refresh-btn" @click="loadMessages(1)">🔄 刷新</button>
            </div>
            <div v-if="msgLoading" class="msg-loading">加载中...</div>
            <div v-else-if="!msgList.length" class="msg-empty">暂无留言，快来第一个留言吧</div>
            <div v-else class="msg-items">
              <div class="msg-item" v-for="m in msgList" :key="m.id">
                <div class="msg-item-header">
                  <div class="msg-avatar">{{ (m.realName || m.userName || '?').charAt(0) }}</div>
                  <div class="msg-meta">
                    <span class="msg-name">{{ m.realName || m.userName }}</span>
                    <span class="msg-time">{{ formatTime(m.createTime) }}</span>
                  </div>
                </div>
                <div class="msg-content">{{ m.content }}</div>
                <div v-if="m.reply" class="msg-reply">
                  <span class="reply-label">官方回复：</span>{{ m.reply }}
                </div>
              </div>
            </div>
            <!-- 分页 -->
            <div v-if="msgTotal > msgPageSize" class="msg-pagination">
              <button :disabled="msgPage <= 1" @click="loadMessages(msgPage - 1)">上一页</button>
              <span>{{ msgPage }} / {{ Math.ceil(msgTotal / msgPageSize) }}</span>
              <button :disabled="msgPage >= Math.ceil(msgTotal / msgPageSize)" @click="loadMessages(msgPage + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据展示带 -->
    <section class="data-band">
      <div class="data-inner">
        <div class="data-item" v-for="d in dataBand" :key="d.label">
          <div class="data-num">{{ d.num }}</div>
          <div class="data-label">{{ d.label }}</div>
        </div>
      </div>
    </section>

    <!-- 底部 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand">
            <div class="nav-emblem small">国</div>
            <div>
              <div class="footer-name">政务服务平台</div>
              <div class="footer-slogan">让政务更简单，让服务更贴心</div>
            </div>
          </div>
          <div class="footer-cols">
            <div class="footer-col">
              <div class="col-title">快速入口</div>
              <a href="#">事项申报</a>
              <a href="#">证件办理</a>
              <a href="#">企业服务</a>
            </div>
            <div class="footer-col">
              <div class="col-title">帮助支持</div>
              <a href="#">使用指南</a>
              <a href="#">常见问题</a>
              <a href="#">联系我们</a>
            </div>
            <div class="footer-col">
              <div class="col-title">关于平台</div>
              <a href="#">平台介绍</a>
              <a href="#">隐私政策</a>
              <a href="#">服务条款</a>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© 2024 政务服务平台 · 保留所有权利</span>
          <span>技术支持：政务信息化中心</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/api/auth'
import { resetDynamicRoutes } from '@/utils/routeHelper'

const router = useRouter()

// ── 登录状态 ──
const isLoggedIn = computed(() => !!localStorage.getItem('access_token'))
const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('user_info') || '{}') } catch { return {} }
})
const avatarChar = computed(() =>
  (userInfo.value.realName || userInfo.value.username || '?').charAt(0).toUpperCase()
)
const roleLabel = computed(() => {
  const r = userInfo.value.roles || []
  if (r.includes('ROLE_SUPER_ADMIN')) return '超级管理员'
  if (r.includes('ROLE_OFFICER')) return '工作人员'
  return '群众用户'
})
async function handleLogout() {
  try { await logout() } catch { }
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user_info')
  localStorage.removeItem('permissions')
  resetDynamicRoutes()
  router.go(0)
}

// ── 轮播 ──
const bannerIndex = ref(0)
const bannerSlides = [
  { tag: '重要通知', title: '数字政务全面升级，服务更便捷', desc: '依托大数据与人工智能技术，打造智慧政务新体验，让群众少跑腿、数据多跑路。', icon: '🏛', bg: 'linear-gradient(135deg, #0f2a52 0%, #1a3a6b 60%, #2d6a9f 100%)' },
  { tag: '政策解读', title: '《数字中国建设整体布局规划》正式发布', desc: '规划明确到2025年基本形成横向打通、纵向贯通的一体化推进格局，数字政务水平大幅提升。', icon: '📋', bg: 'linear-gradient(135deg, #1a3a6b 0%, #0d5c8a 60%, #1a7ab5 100%)' },
  { tag: '便民服务', title: '电子证照跨省互认，42类证件全国通用', desc: '居民身份证、营业执照等42类高频电子证照实现跨省互认，异地办事更方便。', icon: '🪪', bg: 'linear-gradient(135deg, #0a3d62 0%, #1e5f8a 60%, #2980b9 100%)' },
  { tag: '新闻资讯', title: '全国政务服务平台用户突破10亿', desc: '截至2024年，全国一体化政务服务平台注册用户超10亿，年办件量超400亿件次。', icon: '📊', bg: 'linear-gradient(135deg, #1a2a4a 0%, #2c4a7c 60%, #3d6fa8 100%)' },
]
let bannerTimer = null
function nextBanner() { bannerIndex.value = (bannerIndex.value + 1) % bannerSlides.length }
function prevBanner() { bannerIndex.value = (bannerIndex.value - 1 + bannerSlides.length) % bannerSlides.length }
onMounted(() => { bannerTimer = setInterval(nextBanner, 4000) })
onUnmounted(() => clearInterval(bannerTimer))

// ── 快捷入口 ──
const quickLinks = [
  { icon: '📋', label: '事项申报' }, { icon: '🪪', label: '证件办理' },
  { icon: '🏢', label: '企业注册' }, { icon: '💰', label: '税务申报' },
  { icon: '🏥', label: '医保查询' }, { icon: '🎓', label: '学历认证' },
  { icon: '🚗', label: '车辆登记' }, { icon: '🏠', label: '房产登记' },
  { icon: '👶', label: '出生登记' }, { icon: '📦', label: '快递查询' },
]

// ── 办事查询 ──
const queryType = ref('apply')
const queryCode = ref('')
const queryResult = ref(null)
const queryPlaceholder = computed(() => ({
  apply: '请输入申请编号，如：GW2024031500001',
  id: '请输入身份证号码',
  phone: '请输入注册手机号',
}[queryType.value]))
function doQuery() {
  if (!queryCode.value.trim()) return
  // 模拟查询结果
  const results = [
    { status: 'processing', icon: '⏳', text: `申请编号 ${queryCode.value} 正在审核中，预计3个工作日内完成` },
    { status: 'success', icon: '✅', text: `申请编号 ${queryCode.value} 已审核通过，请前往窗口领取` },
    { status: 'pending', icon: '📋', text: `申请编号 ${queryCode.value} 待提交材料，请登录系统补充` },
  ]
  queryResult.value = results[Math.floor(Math.random() * results.length)]
}

// ── 服务大厅 ──
const activeTab = ref('personal')
const serviceTabs = [
  { key: 'personal', icon: '👤', label: '个人服务' },
  { key: 'enterprise', icon: '🏢', label: '企业服务' },
  { key: 'special', icon: '⭐', label: '特色服务' },
]
const allServices = {
  personal: [
    { icon: '🪪', title: '居民身份证', desc: '申请、补办、换领居民身份证', time: '5个工作日', color: '#e6f4ff', hot: true },
    { icon: '🏠', title: '户籍迁移', desc: '户口迁入迁出、分户合户办理', time: '10个工作日', color: '#f6ffed', hot: false },
    { icon: '💍', title: '婚姻登记', desc: '结婚登记、离婚登记、补领证书', time: '当日办结', color: '#fff7e6', hot: true },
    { icon: '👶', title: '出生登记', desc: '新生儿出生医学证明及户籍登记', time: '3个工作日', color: '#f9f0ff', hot: false },
    { icon: '🏥', title: '医保办理', desc: '医保参保、转移、待遇申请', time: '5个工作日', color: '#fff2f0', hot: true },
    { icon: '🎓', title: '学历认证', desc: '国内外学历学位认证查询', time: '15个工作日', color: '#e6fffb', hot: false },
    { icon: '🚗', title: '驾照申请', desc: '驾驶证申请、换证、补证', time: '20个工作日', color: '#feffe6', hot: false },
    { icon: '✈️', title: '护照办理', desc: '普通护照申请及换补领', time: '15个工作日', color: '#e6f4ff', hot: true },
  ],
  enterprise: [
    { icon: '📝', title: '营业执照', desc: '企业设立、变更、注销登记', time: '3个工作日', color: '#e6f4ff', hot: true },
    { icon: '🏭', title: '生产许可', desc: '工业产品生产许可证申请', time: '20个工作日', color: '#f6ffed', hot: false },
    { icon: '💼', title: '资质认证', desc: '企业资质等级认定与年审', time: '30个工作日', color: '#fff7e6', hot: false },
    { icon: '📊', title: '税务登记', desc: '企业税务登记、变更、注销', time: '5个工作日', color: '#f9f0ff', hot: true },
    { icon: '🌐', title: '进出口资质', desc: '进出口经营资格备案登记', time: '10个工作日', color: '#fff2f0', hot: false },
    { icon: '🔒', title: '安全生产', desc: '安全生产许可证申请与年审', time: '20个工作日', color: '#e6fffb', hot: false },
    { icon: '🏗', title: '建设许可', desc: '建设工程规划许可证申请', time: '15个工作日', color: '#feffe6', hot: true },
    { icon: '♻️', title: '环保审批', desc: '建设项目环境影响评价审批', time: '30个工作日', color: '#e6f4ff', hot: false },
  ],
  special: [
    { icon: '🎖', title: '退役军人服务', desc: '退役军人优待证申请及权益保障', time: '10个工作日', color: '#fff7e6', hot: true },
    { icon: '♿', title: '残疾人服务', desc: '残疾证申请、补贴申领', time: '15个工作日', color: '#f6ffed', hot: false },
    { icon: '👴', title: '老年人服务', desc: '老年优待证、养老补贴申请', time: '5个工作日', color: '#e6f4ff', hot: true },
    { icon: '🌾', title: '农业补贴', desc: '种粮补贴、农机购置补贴申请', time: '30个工作日', color: '#feffe6', hot: false },
    { icon: '🏘', title: '保障性住房', desc: '公租房、廉租房申请与轮候', time: '60个工作日', color: '#f9f0ff', hot: true },
    { icon: '📚', title: '教育资助', desc: '助学金、奖学金、助学贷款', time: '20个工作日', color: '#e6fffb', hot: false },
    { icon: '💊', title: '慢病管理', desc: '慢性病患者建档及用药保障', time: '5个工作日', color: '#fff2f0', hot: false },
    { icon: '🌍', title: '来华外籍服务', desc: '外籍人员居留许可、工作许可', time: '15个工作日', color: '#e6f4ff', hot: false },
  ],
}
const currentServices = computed(() => allServices[activeTab.value])

// ── 新闻 ──
const newsList = [
  { icon: '🏛', title: '国务院印发《数字中国建设整体布局规划》，推进政务数字化转型', summary: '规划明确提出，到2025年，基本形成横向打通、纵向贯通、协调有力的一体化推进格局，数字中国建设取得重要进展。', source: '国务院新闻办', date: '2024-03-15', tagClass: 'hot' },
  { icon: '📋', title: '全国政务服务平台用户突破10亿，服务能力持续提升', source: '人民日报', date: '2024-03-14', tagClass: 'policy' },
  { icon: '🪪', title: '电子证照跨省互认取得新突破，42类证照实现全国通用', source: '新华社', date: '2024-03-13', tagClass: 'notice' },
  { icon: '📊', title: '2024年政府工作报告：深化"放管服"改革，优化营商环境', source: '中国政府网', date: '2024-03-12', tagClass: 'policy' },
  { icon: '⚖️', title: '数据安全法实施两周年：个人信息保护水平显著提升', source: '法制日报', date: '2024-03-11', tagClass: 'law' },
]

const notices = [
  { text: '关于2024年度政务服务平台系统升级维护的通知', date: '2024-03-15' },
  { text: '关于开展政务服务"好差评"专项整治工作的通知', date: '2024-03-12' },
  { text: '关于调整部分政务服务事项办理流程的公告', date: '2024-03-10' },
  { text: '关于加强政务数据安全管理工作的通知', date: '2024-03-08' },
  { text: '2024年第一季度政务服务质量评估报告发布', date: '2024-03-05' },
]

const policies = [
  { title: '《政务数据共享条例》（征求意见稿）', date: '2024-03' },
  { title: '《网络安全法》修订草案公开征求意见', date: '2024-02' },
  { title: '《个人信息保护法》实施细则发布', date: '2024-01' },
  { title: '《数字经济促进条例》正式施行', date: '2023-12' },
]

// ── 留言板 ──
import { getMessages, submitMessage } from '@/api/auth'
const msgList = ref([])
const msgTotal = ref(0)
const msgPage = ref(1)
const msgPageSize = 8
const msgLoading = ref(false)
const msgContent = ref('')
const msgSubmitting = ref(false)
const msgSuccess = ref(false)
const msgError = ref('')

async function loadMessages(page = 1) {
  msgLoading.value = true
  msgPage.value = page
  try {
    const res = await getMessages({ page, size: msgPageSize })
    msgList.value = res.data.data?.list || []
    msgTotal.value = res.data.data?.total || 0
  } catch { msgList.value = [] } finally { msgLoading.value = false }
}

async function submitMsg() {
  if (!msgContent.value.trim()) return
  msgSubmitting.value = true
  msgSuccess.value = false
  msgError.value = ''
  try {
    await submitMessage(msgContent.value)
    msgContent.value = ''
    msgSuccess.value = true
  } catch (e) {
    msgError.value = e?.response?.data?.message || '提交失败，请稍后重试'
  } finally { msgSubmitting.value = false }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => { loadMessages() })

const dataBand = [
  { num: '50万+', label: '注册用户' },
  { num: '200+', label: '政务事项' },
  { num: '400亿', label: '年办件量' },
  { num: '99.9%', label: '服务可用率' },
  { num: '24h', label: '全天候服务' },
  { num: '31', label: '覆盖省市' },
]
</script>

<style scoped>
.portal { min-height: 100vh; background: #f0f2f5; font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; }

/* ── 导航 ── */
.nav { position: sticky; top: 0; z-index: 100; background: rgba(15,42,82,0.97); backdrop-filter: blur(8px); box-shadow: 0 2px 12px rgba(0,0,0,0.2); }
.nav-inner { max-width: 1280px; margin: 0 auto; padding: 0 24px; height: 64px; display: flex; align-items: center; gap: 32px; }
.nav-brand { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.nav-emblem { width: 40px; height: 40px; border-radius: 10px; background: linear-gradient(135deg, #c8a84b, #e8c96a); display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 900; color: #0f2a52; }
.nav-emblem.small { width: 32px; height: 32px; font-size: 14px; border-radius: 7px; }
.nav-title { font-size: 16px; font-weight: 700; color: #fff; }
.nav-sub { font-size: 10px; color: rgba(255,255,255,0.4); }
.nav-links { display: flex; gap: 4px; flex: 1; }
.nav-links a { color: rgba(255,255,255,0.7); text-decoration: none; font-size: 14px; padding: 6px 14px; border-radius: 6px; transition: all 0.15s; }
.nav-links a:hover { background: rgba(255,255,255,0.1); color: #fff; }
.nav-actions { display: flex; gap: 10px; flex-shrink: 0; }
.nav-user { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.nav-avatar { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #c8a84b, #e8c96a); color: #0f2a52; font-size: 14px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.nav-user-info { display: flex; flex-direction: column; }
.nav-username { font-size: 13px; font-weight: 600; color: #fff; line-height: 1.3; }
.nav-role { font-size: 11px; color: rgba(255,255,255,0.5); }
.btn-login { padding: 7px 18px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.3); color: #fff; text-decoration: none; font-size: 14px; transition: all 0.15s; }
.btn-login:hover { background: rgba(255,255,255,0.1); }
.btn-register { padding: 7px 18px; border-radius: 6px; background: linear-gradient(135deg, #c8a84b, #e8c96a); color: #0f2a52; text-decoration: none; font-size: 14px; font-weight: 600; transition: opacity 0.15s; }
.btn-register:hover { opacity: 0.9; }
.btn-logout { padding: 7px 14px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.2); background: none; color: rgba(255,255,255,0.7); font-size: 13px; cursor: pointer; transition: all 0.15s; }
.btn-logout:hover { background: rgba(255,255,255,0.1); color: #fff; }

/* ── 轮播 ── */
.banner { position: relative; overflow: hidden; height: 420px; }
.banner-track { display: flex; height: 100%; transition: transform 0.5s cubic-bezier(.4,0,.2,1); }
.banner-slide { min-width: 100%; height: 100%; display: flex; align-items: center; padding: 0 10%; position: relative; overflow: hidden; }
.banner-content { position: relative; z-index: 2; max-width: 560px; }
.banner-tag { display: inline-block; background: rgba(200,168,75,0.25); border: 1px solid rgba(200,168,75,0.5); color: #e8c96a; padding: 4px 14px; border-radius: 20px; font-size: 13px; margin-bottom: 16px; }
.banner-content h2 { font-size: 36px; font-weight: 800; color: #fff; margin: 0 0 14px; line-height: 1.3; }
.banner-content p { font-size: 15px; color: rgba(255,255,255,0.75); margin: 0 0 28px; line-height: 1.8; }
.banner-btn { display: inline-block; padding: 12px 32px; background: linear-gradient(135deg, #c8a84b, #e8c96a); color: #0f2a52; border-radius: 8px; text-decoration: none; font-size: 15px; font-weight: 700; transition: opacity 0.15s; }
.banner-btn:hover { opacity: 0.9; }
.banner-deco { position: absolute; right: 8%; font-size: 160px; opacity: 0.08; pointer-events: none; }
.banner-dots { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.4); cursor: pointer; transition: all 0.2s; }
.dot.active { background: #e8c96a; width: 24px; border-radius: 4px; }
.banner-arrow { position: absolute; top: 50%; transform: translateY(-50%); background: rgba(255,255,255,0.15); border: none; color: #fff; font-size: 28px; width: 44px; height: 44px; border-radius: 50%; cursor: pointer; transition: background 0.15s; display: flex; align-items: center; justify-content: center; }
.banner-arrow:hover { background: rgba(255,255,255,0.3); }
.banner-arrow.left { left: 20px; }
.banner-arrow.right { right: 20px; }

/* ── 快捷入口 ── */
.quick-bar { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.quick-inner { max-width: 1280px; margin: 0 auto; padding: 0 24px; display: flex; overflow-x: auto; }
.quick-item { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px 20px; cursor: pointer; transition: background 0.15s; white-space: nowrap; flex-shrink: 0; }
.quick-item:hover { background: #f0f4ff; }
.quick-icon { font-size: 22px; }
.quick-item span:last-child { font-size: 12px; color: #555; }

/* ── 查询 ── */
.query-section { padding: 32px 0; background: #f0f2f5; }
.section-inner { max-width: 1280px; margin: 0 auto; padding: 0 24px; }
.query-card { background: linear-gradient(135deg, #1a3a6b, #2d6a9f); border-radius: 14px; padding: 28px 36px; display: flex; align-items: center; gap: 40px; }
.query-left h3 { margin: 0 0 8px; font-size: 20px; color: #fff; }
.query-left p { margin: 0; font-size: 13px; color: rgba(255,255,255,0.7); }
.query-right { flex: 1; }
.query-input-group { display: flex; gap: 0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.2); }
.query-input-group select { height: 46px; padding: 0 14px; border: none; background: #fff; font-size: 13px; color: #333; outline: none; border-right: 1px solid #e8e8e8; cursor: pointer; }
.query-input-group input { flex: 1; height: 46px; padding: 0 16px; border: none; font-size: 14px; outline: none; }
.query-input-group button { height: 46px; padding: 0 28px; background: linear-gradient(135deg, #c8a84b, #e8c96a); border: none; color: #0f2a52; font-size: 15px; font-weight: 700; cursor: pointer; transition: opacity 0.15s; }
.query-input-group button:hover { opacity: 0.9; }
.query-result { margin-top: 12px; padding: 10px 16px; border-radius: 8px; font-size: 13px; display: flex; align-items: center; gap: 8px; }
.query-result.processing { background: rgba(255,255,255,0.15); color: #fff; }
.query-result.success { background: rgba(82,196,26,0.2); color: #d4f7b0; }
.query-result.pending { background: rgba(250,173,20,0.2); color: #ffe58f; }

/* ── 服务大厅 ── */
.services { padding: 56px 0; background: #fff; }
.section-header { text-align: center; margin-bottom: 32px; }
.section-header h2 { font-size: 28px; font-weight: 700; color: #1a3a6b; margin: 0 0 8px; }
.section-header p { font-size: 15px; color: #888; margin: 0; }
.service-tabs { display: flex; gap: 8px; margin-bottom: 24px; justify-content: center; }
.tab-btn { padding: 8px 24px; border-radius: 20px; border: 1px solid #d0d7de; background: #fff; font-size: 14px; cursor: pointer; color: #555; transition: all 0.15s; }
.tab-btn:hover { border-color: #2d6a9f; color: #2d6a9f; }
.tab-btn.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }
.service-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1000px) { .service-grid { grid-template-columns: repeat(2, 1fr); } }
.service-card { display: flex; gap: 14px; background: #f8f9fb; border-radius: 12px; padding: 18px; transition: all 0.2s; cursor: pointer; border: 1px solid transparent; }
.service-card:hover { background: #fff; border-color: #c5d5f0; box-shadow: 0 4px 16px rgba(26,58,107,0.1); transform: translateY(-2px); }
.service-icon-wrap { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.service-title { font-size: 14px; font-weight: 600; color: #1a3a6b; margin-bottom: 4px; }
.service-desc { font-size: 12px; color: #888; line-height: 1.5; margin-bottom: 8px; }
.service-meta { display: flex; align-items: center; gap: 10px; }
.service-time { font-size: 11px; color: #aaa; }
.service-hot { font-size: 11px; color: #d4380d; }

/* ── 新闻区 ── */
.news-section { padding: 56px 0; background: #f0f2f5; }
.news-notice-grid { display: grid; grid-template-columns: 1fr 420px; gap: 24px; }
@media (max-width: 1000px) { .news-notice-grid { grid-template-columns: 1fr; } }
.block-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.block-header h2 { font-size: 18px; font-weight: 700; color: #1a3a6b; margin: 0; }
.more-link { font-size: 13px; color: #2d6a9f; text-decoration: none; }
.more-link:hover { text-decoration: underline; }

.news-block { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.news-featured-card { display: flex; gap: 16px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.featured-cover { width: 120px; height: 90px; border-radius: 8px; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); display: flex; align-items: center; justify-content: center; position: relative; flex-shrink: 0; }
.cover-bg { font-size: 40px; opacity: 0.4; }
.featured-tag { position: absolute; top: 6px; left: 6px; background: #cf1322; color: #fff; font-size: 11px; padding: 2px 7px; border-radius: 4px; }
.featured-body h4 { margin: 0 0 8px; font-size: 15px; font-weight: 600; color: #1a3a6b; line-height: 1.5; }
.featured-body p { margin: 0 0 10px; font-size: 12px; color: #666; line-height: 1.7; }
.news-meta-row { display: flex; justify-content: space-between; font-size: 11px; color: #aaa; }

.news-sub-list { display: flex; flex-direction: column; gap: 0; }
.news-sub-item { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; align-items: flex-start; }
.news-sub-item:last-child { border-bottom: none; }
.tag-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.tag-dot.hot { background: #cf1322; }
.tag-dot.policy { background: #0958d9; }
.tag-dot.notice { background: #389e0d; }
.tag-dot.law { background: #531dab; }
.sub-title { font-size: 13px; color: #333; line-height: 1.5; margin-bottom: 3px; }
.sub-meta { font-size: 11px; color: #aaa; }

.side-blocks { display: flex; flex-direction: column; gap: 16px; }
.notice-block, .policy-block { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.notice-list, .policy-list { display: flex; flex-direction: column; }
.notice-item { display: flex; gap: 10px; padding: 9px 0; border-bottom: 1px solid #f5f5f5; align-items: flex-start; }
.notice-item:last-child { border-bottom: none; }
.notice-dot { width: 6px; height: 6px; border-radius: 50%; background: #c8a84b; flex-shrink: 0; margin-top: 6px; }
.notice-text { font-size: 13px; color: #333; line-height: 1.5; flex: 1; }
.notice-date { font-size: 11px; color: #aaa; flex-shrink: 0; }
.policy-item { display: flex; gap: 12px; padding: 9px 0; border-bottom: 1px solid #f5f5f5; align-items: center; }
.policy-item:last-child { border-bottom: none; }
.policy-date { font-size: 11px; color: #aaa; flex-shrink: 0; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; }
.policy-title { font-size: 13px; color: #333; }

/* ── 留言板 ── */
.message-section { padding: 56px 0; background: #fff; }
.message-layout { display: grid; grid-template-columns: 380px 1fr; gap: 24px; }
@media (max-width: 900px) { .message-layout { grid-template-columns: 1fr; } }

.message-form-card { background: #f8f9fb; border-radius: 12px; padding: 24px; border: 1px solid #e8e8e8; }
.form-card-title { font-size: 16px; font-weight: 700; color: #1a3a6b; margin-bottom: 16px; }
.form-user-info { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; font-size: 13px; color: #555; }
.form-avatar { width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); color: #fff; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.message-form-card textarea { width: 100%; padding: 12px; border: 1px solid #d0d7de; border-radius: 8px; font-size: 14px; resize: vertical; outline: none; font-family: inherit; box-sizing: border-box; }
.message-form-card textarea:focus { border-color: #2d6a9f; box-shadow: 0 0 0 3px rgba(45,106,159,0.1); }
.form-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.char-count { font-size: 12px; color: #aaa; }
.submit-msg-btn { background: #1a3a6b; color: #fff; border: none; border-radius: 7px; padding: 8px 24px; font-size: 14px; cursor: pointer; transition: background 0.15s; }
.submit-msg-btn:hover:not(:disabled) { background: #2d6a9f; }
.submit-msg-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.msg-tip { margin-top: 12px; padding: 10px 14px; border-radius: 8px; font-size: 13px; }
.msg-tip.success { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.msg-tip.error { background: #fff2f0; color: #cf1322; border: 1px solid #ffccc7; }
.login-tip { text-align: center; padding: 32px 0; }
.login-tip span { font-size: 40px; display: block; margin-bottom: 12px; }
.login-tip p { color: #888; font-size: 14px; margin: 0 0 16px; }

.message-list-card { background: #f8f9fb; border-radius: 12px; padding: 20px; border: 1px solid #e8e8e8; }
.list-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; font-size: 14px; font-weight: 600; color: #1a3a6b; }
.refresh-btn { background: none; border: 1px solid #d0d7de; border-radius: 6px; padding: 4px 12px; font-size: 12px; cursor: pointer; color: #555; }
.refresh-btn:hover { background: #f0f0f0; }
.msg-loading, .msg-empty { text-align: center; color: #aaa; padding: 32px; font-size: 14px; }
.msg-items { display: flex; flex-direction: column; gap: 12px; }
.msg-item { background: #fff; border-radius: 10px; padding: 14px 16px; border: 1px solid #f0f0f0; }
.msg-item-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); color: #fff; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.msg-name { font-size: 13px; font-weight: 600; color: #333; }
.msg-time { font-size: 11px; color: #aaa; margin-left: 8px; }
.msg-content { font-size: 14px; color: #333; line-height: 1.7; }
.msg-reply { margin-top: 10px; background: #f0f4ff; border-left: 3px solid #1a3a6b; padding: 8px 12px; border-radius: 0 6px 6px 0; font-size: 13px; color: #1a3a6b; }
.reply-label { font-weight: 600; margin-right: 4px; }
.msg-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 16px; font-size: 13px; color: #555; }
.msg-pagination button { padding: 4px 14px; border: 1px solid #d0d7de; border-radius: 6px; cursor: pointer; background: #fff; }
.msg-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 数据带 ── */
.data-band { background: linear-gradient(135deg, #0f2a52, #1a3a6b); padding: 40px 24px; }
.data-inner { max-width: 1280px; margin: 0 auto; display: flex; justify-content: space-around; flex-wrap: wrap; gap: 24px; }
.data-item { text-align: center; }
.data-num { font-size: 36px; font-weight: 800; color: #e8c96a; line-height: 1.2; }
.data-label { font-size: 13px; color: rgba(255,255,255,0.6); margin-top: 4px; }

/* ── 底部 ── */
.footer { background: #0a1f3d; color: rgba(255,255,255,0.6); padding: 40px 24px 20px; }
.footer-inner { max-width: 1280px; margin: 0 auto; }
.footer-top { display: flex; justify-content: space-between; gap: 40px; margin-bottom: 32px; flex-wrap: wrap; }
.footer-brand { display: flex; align-items: flex-start; gap: 12px; }
.footer-name { font-size: 16px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.footer-slogan { font-size: 12px; color: rgba(255,255,255,0.4); }
.footer-cols { display: flex; gap: 48px; }
.footer-col { display: flex; flex-direction: column; gap: 10px; }
.col-title { font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.8); margin-bottom: 4px; }
.footer-col a { font-size: 13px; color: rgba(255,255,255,0.45); text-decoration: none; transition: color 0.15s; }
.footer-col a:hover { color: #fff; }
.footer-bottom { border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; display: flex; justify-content: space-between; font-size: 12px; flex-wrap: wrap; gap: 8px; }
</style>
