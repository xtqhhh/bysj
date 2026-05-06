<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>政务申请管理</h2>
        <p class="page-desc">受理、审核群众提交的政务申请事项</p>
      </div>
    </div>

    <!-- 状态 Tab -->
    <div class="tab-bar">
      <button v-for="t in tabs" :key="t.value"
        :class="['tab-btn', { active: filterStatus === t.value }]"
        @click="filterStatus = t.value; page = 1; fetchList()">
        {{ t.label }}
        <span v-if="t.value === null" class="tab-count">{{ total }}</span>
      </button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label>申请类型</label>
          <select v-model="filterType" @change="fetchList()">
            <option value="">全部类型</option>
            <option v-for="t in serviceTypes" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn-default" @click="resetFilter">重置</button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-total">共 <b>{{ total }}</b> 条申请</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>申请编号</th>
            <th>申请人</th>
            <th>申请类型</th>
            <th>申请标题</th>
            <th>申请时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty-row">加载中...</td></tr>
          <tr v-else-if="!list.length"><td colspan="7" class="empty-row">暂无申请数据</td></tr>
          <tr v-for="row in list" :key="row.id" class="data-row">
            <td class="mono-cell">{{ row.applyNo }}</td>
            <td>
              <div class="user-cell">
                <div class="mini-avatar">{{ (row.realName || row.userName || '?').charAt(0) }}</div>
                <div>
                  <div class="user-name">{{ row.realName || row.userName }}</div>
                  <div class="user-sub">{{ row.userName }}</div>
                </div>
              </div>
            </td>
            <td><span class="type-tag">{{ row.type }}</span></td>
            <td class="title-cell">{{ row.title }}</td>
            <td class="time-cell">{{ formatTime(row.createTime) }}</td>
            <td><span :class="['status-badge', statusClass(row.status)]">{{ statusLabel(row.status) }}</span></td>
            <td>
              <div class="action-group">
                <button class="action-btn" @click="viewDetail(row)">查看</button>
                <template v-if="row.status === 0">
                  <button class="action-btn accept" @click="quickReview(row, 1)">受理</button>
                </template>
                <template v-if="row.status === 1">
                  <button class="action-btn approve" @click="openReview(row, 2)">通过</button>
                  <button class="action-btn reject" @click="openReview(row, 3)">拒绝</button>
                </template>
                <template v-if="row.status === 2">
                  <button class="action-btn done" @click="quickReview(row, 4)">完成</button>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <div class="page-btns">
          <button :disabled="page <= 1" @click="page--; fetchList()">‹ 上一页</button>
          <button v-for="p in pageRange" :key="p"
            :class="['page-num', { active: p === page }]"
            @click="page = p; fetchList()">{{ p }}</button>
          <button :disabled="page >= totalPages" @click="page++; fetchList()">下一页 ›</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailRow" class="modal-mask" @click.self="detailRow = null">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h3>申请详情</h3>
          <button class="modal-close" @click="detailRow = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">申请编号</span><span class="detail-value mono">{{ detailRow.applyNo }}</span></div>
            <div class="detail-item"><span class="detail-label">申请人</span><span class="detail-value">{{ detailRow.realName || detailRow.userName }}</span></div>
            <div class="detail-item"><span class="detail-label">申请类型</span><span class="detail-value">{{ detailRow.type }}</span></div>
            <div class="detail-item"><span class="detail-label">申请时间</span><span class="detail-value">{{ formatTime(detailRow.createTime) }}</span></div>
            <div class="detail-item"><span class="detail-label">当前状态</span>
              <span :class="['status-badge', statusClass(detailRow.status)]">{{ statusLabel(detailRow.status) }}</span>
            </div>
            <div v-if="detailRow.updateTime" class="detail-item">
              <span class="detail-label">最后更新</span>
              <span class="detail-value">{{ formatTime(detailRow.updateTime) }}</span>
            </div>
          </div>
          <div class="detail-full">
            <span class="detail-label">申请标题</span>
            <div class="detail-content">{{ detailRow.title }}</div>
          </div>
          <div v-if="detailRow.description" class="detail-full">
            <span class="detail-label">详细描述</span>
            <div class="detail-content">{{ detailRow.description }}</div>
          </div>
          <!-- 进度时间线 -->
          <div class="timeline">
            <div class="timeline-title">办理进度</div>
            <div class="timeline-steps">
              <div v-for="(step, i) in progressSteps" :key="i"
                :class="['tl-step', { done: detailRow.status > i && detailRow.status < 5, active: detailRow.status === i, pending: detailRow.status < i }]">
                <div class="tl-dot">{{ detailRow.status > i && detailRow.status < 5 ? '✓' : i + 1 }}</div>
                <div class="tl-info">
                  <div class="tl-name">{{ step }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="detailRow.remark" class="remark-box">
            <span class="remark-label">审核备注：</span>{{ detailRow.remark }}
          </div>
        </div>
        <div class="modal-footer">
          <template v-if="detailRow.status === 0">
            <button class="btn-accept" @click="quickReview(detailRow, 1); detailRow = null">受理申请</button>
          </template>
          <template v-if="detailRow.status === 1">
            <button class="btn-reject" @click="openReview(detailRow, 3); detailRow = null">拒绝</button>
            <button class="btn-approve" @click="openReview(detailRow, 2); detailRow = null">审核通过</button>
          </template>
          <template v-if="detailRow.status === 2">
            <button class="btn-approve" @click="quickReview(detailRow, 4); detailRow = null">标记完成</button>
          </template>
          <button class="btn-default" @click="detailRow = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 审核弹窗（通过/拒绝，需填写备注） -->
    <div v-if="reviewModal.show" class="modal-mask" @click.self="reviewModal.show = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ reviewModal.targetStatus === 2 ? '审核通过' : '拒绝申请' }}</h3>
          <button class="modal-close" @click="reviewModal.show = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="review-info">
            申请编号：<b>{{ reviewModal.row?.applyNo }}</b><br>
            申请标题：{{ reviewModal.row?.title }}
          </div>
          <div class="form-group">
            <label>审核备注 {{ reviewModal.targetStatus === 3 ? '（必填，请说明拒绝原因）' : '（选填）' }}</label>
            <textarea v-model="reviewModal.remark" rows="4"
              :placeholder="reviewModal.targetStatus === 3 ? '请填写拒绝原因...' : '请填写审核意见（选填）...'"
            ></textarea>
          </div>
          <div v-if="reviewModal.error" class="form-error">{{ reviewModal.error }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="reviewModal.show = false">取消</button>
          <button :class="reviewModal.targetStatus === 2 ? 'btn-approve' : 'btn-reject'"
            @click="submitReview" :disabled="reviewModal.submitting">
            {{ reviewModal.submitting ? '提交中...' : (reviewModal.targetStatus === 2 ? '确认通过' : '确认拒绝') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getApplicationsAdmin, reviewApplication } from '@/api/admin'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = 10
const filterStatus = ref(null)
const filterType = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const pageRange = computed(() => {
  const pages = []
  const start = Math.max(1, page.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const tabs = [
  { label: '全部', value: null },
  { label: '⏳ 待受理', value: 0 },
  { label: '🔄 审核中', value: 1 },
  { label: '✅ 已通过', value: 2 },
  { label: '❌ 已拒绝', value: 3 },
  { label: '🏁 已完成', value: 4 },
  { label: '🚫 已撤销', value: 5 },
]

const serviceTypes = ['身份证办理', '户籍迁移', '婚姻登记', '出生登记', '医保办理', '学历认证', '驾照申请', '其他事项']

const progressSteps = ['待受理', '审核中', '已通过', '已完成']

const STATUS_MAP = {
  0: { label: '待受理', cls: 'pending' },
  1: { label: '审核中', cls: 'processing' },
  2: { label: '已通过', cls: 'approved' },
  3: { label: '已拒绝', cls: 'rejected' },
  4: { label: '已完成', cls: 'done' },
  5: { label: '已撤销', cls: 'cancelled' },
}
const statusLabel = s => STATUS_MAP[s]?.label || s
const statusClass = s => STATUS_MAP[s]?.cls || ''

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getApplicationsAdmin({ status: filterStatus.value, type: filterType.value || undefined, page: page.value, size })
    list.value = res.data.data?.list || []
    total.value = res.data.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function resetFilter() {
  filterStatus.value = null
  filterType.value = ''
  page.value = 1
  fetchList()
}

// 详情
const detailRow = ref(null)
function viewDetail(row) { detailRow.value = { ...row } }

// 快速审核（受理、完成，不需要备注）
async function quickReview(row, status) {
  try {
    await reviewApplication(row.id, { status, remark: '' })
    row.status = status
    fetchList()
  } catch (e) {
    alert(e?.response?.data?.message || '操作失败')
  }
}

// 审核弹窗（通过/拒绝，需填写备注）
const reviewModal = reactive({ show: false, row: null, targetStatus: 2, remark: '', error: '', submitting: false })

function openReview(row, targetStatus) {
  reviewModal.row = row
  reviewModal.targetStatus = targetStatus
  reviewModal.remark = ''
  reviewModal.error = ''
  reviewModal.show = true
}

async function submitReview() {
  if (reviewModal.targetStatus === 3 && !reviewModal.remark.trim()) {
    reviewModal.error = '拒绝时必须填写原因'
    return
  }
  reviewModal.submitting = true
  reviewModal.error = ''
  try {
    await reviewApplication(reviewModal.row.id, { status: reviewModal.targetStatus, remark: reviewModal.remark })
    reviewModal.row.status = reviewModal.targetStatus
    reviewModal.show = false
    fetchList()
  } catch (e) {
    reviewModal.error = e?.response?.data?.message || '操作失败'
  } finally { reviewModal.submitting = false }
}

onMounted(fetchList)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 4px; font-size: 18px; color: #1a3a6b; }
.page-desc { margin: 0; font-size: 13px; color: #888; }

.tab-bar { display: flex; gap: 6px; flex-wrap: wrap; }
.tab-btn { padding: 6px 16px; border-radius: 20px; border: 1px solid #d0d7de; background: #fff; font-size: 13px; cursor: pointer; color: #555; transition: all 0.15s; display: flex; align-items: center; gap: 5px; }
.tab-btn:hover { border-color: #2d6a9f; color: #2d6a9f; }
.tab-btn.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }
.tab-count { background: rgba(255,255,255,0.2); padding: 1px 6px; border-radius: 10px; font-size: 11px; }

.filter-card { background: #fff; border-radius: 10px; padding: 14px 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.filter-row { display: flex; gap: 16px; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 5px; }
.filter-item label { font-size: 12px; color: #666; }
.filter-item select { height: 34px; padding: 0 10px; border: 1px solid #d0d7de; border-radius: 6px; font-size: 13px; outline: none; min-width: 140px; }
.filter-actions { display: flex; gap: 8px; }

.table-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); overflow: hidden; }
.table-toolbar { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
.table-total { font-size: 13px; color: #666; }
.table-total b { color: #1a3a6b; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th { background: #f8f9fb; padding: 11px 14px; text-align: left; color: #555; font-weight: 600; font-size: 12px; border-bottom: 1px solid #eee; }
td { padding: 12px 14px; border-bottom: 1px solid #f5f5f5; color: #333; vertical-align: middle; }
.data-row:hover td { background: #fafbff; }
.data-row:last-child td { border-bottom: none; }
.empty-row { text-align: center; color: #bbb; padding: 40px; font-size: 14px; }
.mono-cell { font-family: monospace; font-size: 12px; color: #666; }
.time-cell { color: #999; font-size: 12px; white-space: nowrap; }
.title-cell { max-width: 220px; }

.user-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.user-name { font-size: 13px; font-weight: 500; }
.user-sub { font-size: 11px; color: #aaa; }

.type-tag { background: #e6f4ff; color: #0958d9; padding: 2px 8px; border-radius: 8px; font-size: 11px; white-space: nowrap; }

.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-badge.pending { background: #fff7e6; color: #d46b08; }
.status-badge.processing { background: #e6f4ff; color: #0958d9; }
.status-badge.approved { background: #f0fff4; color: #22863a; }
.status-badge.rejected { background: #fff0f0; color: #cb2431; }
.status-badge.done { background: #f6ffed; color: #389e0d; }
.status-badge.cancelled { background: #f5f5f5; color: #999; }

.action-group { display: flex; gap: 6px; flex-wrap: wrap; }
.action-btn { background: none; border: 1px solid #d0d7de; border-radius: 5px; padding: 3px 10px; font-size: 12px; cursor: pointer; color: #2d6a9f; transition: all 0.15s; }
.action-btn:hover { background: #f0f4ff; border-color: #2d6a9f; }
.action-btn.accept { color: #0958d9; border-color: #91caff; }
.action-btn.accept:hover { background: #e6f4ff; }
.action-btn.approve { color: #389e0d; border-color: #b7eb8f; }
.action-btn.approve:hover { background: #f6ffed; }
.action-btn.reject { color: #cf1322; border-color: #ffccc7; }
.action-btn.reject:hover { background: #fff2f0; }
.action-btn.done { color: #389e0d; border-color: #b7eb8f; }
.action-btn.done:hover { background: #f6ffed; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.page-info { font-size: 13px; color: #888; }
.page-btns { display: flex; gap: 4px; }
.page-btns button { padding: 4px 10px; border: 1px solid #d0d7de; border-radius: 5px; cursor: pointer; background: #fff; font-size: 13px; color: #555; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btns button:hover:not(:disabled) { border-color: #2d6a9f; color: #2d6a9f; }
.page-num.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 12px; width: 480px; box-shadow: 0 8px 32px rgba(0,0,0,0.2); max-height: 85vh; overflow-y: auto; }
.modal-wide { width: 600px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px 0; position: sticky; top: 0; background: #fff; }
.modal-header h3 { margin: 0; font-size: 16px; color: #1a3a6b; }
.modal-close { background: none; border: none; font-size: 16px; cursor: pointer; color: #aaa; }
.modal-body { padding: 16px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 0 24px 20px; position: sticky; bottom: 0; background: #fff; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 11px; color: #aaa; text-transform: uppercase; letter-spacing: 0.5px; }
.detail-value { font-size: 14px; color: #333; font-weight: 500; }
.detail-value.mono { font-family: monospace; font-size: 13px; }
.detail-full { margin-bottom: 14px; }
.detail-content { background: #f8f9fb; border-radius: 8px; padding: 12px; font-size: 14px; color: #333; line-height: 1.7; margin-top: 6px; }

.timeline { margin: 16px 0; }
.timeline-title { font-size: 13px; font-weight: 600; color: #555; margin-bottom: 16px; }
.timeline-steps { display: flex; }
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

.remark-box { background: #fff7e6; border: 1px solid #ffe58f; border-radius: 8px; padding: 10px 14px; font-size: 13px; color: #7a5c00; margin-top: 12px; }
.remark-label { font-weight: 600; margin-right: 4px; }

.review-info { background: #f8f9fb; border-radius: 8px; padding: 12px; font-size: 13px; color: #333; line-height: 1.8; margin-bottom: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; color: #555; font-weight: 500; }
.form-group textarea { padding: 10px; border: 1px solid #d0d7de; border-radius: 7px; font-size: 13px; outline: none; resize: vertical; font-family: inherit; }
.form-group textarea:focus { border-color: #2d6a9f; }
.form-error { color: #cf1322; font-size: 13px; margin-top: 8px; }

.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 7px; padding: 6px 18px; font-size: 13px; cursor: pointer; }
.btn-approve { background: #389e0d; color: #fff; border: none; border-radius: 7px; padding: 7px 20px; font-size: 13px; cursor: pointer; }
.btn-approve:hover:not(:disabled) { background: #52c41a; }
.btn-approve:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-reject { background: #cf1322; color: #fff; border: none; border-radius: 7px; padding: 7px 20px; font-size: 13px; cursor: pointer; }
.btn-reject:hover:not(:disabled) { background: #ff4d4f; }
.btn-reject:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-accept { background: #0958d9; color: #fff; border: none; border-radius: 7px; padding: 7px 20px; font-size: 13px; cursor: pointer; }
.btn-accept:hover { background: #1677ff; }
</style>
