<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>留言管理</h2>
        <p class="page-desc">审核用户留言，通过后将在门户首页公开展示</p>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="tab-bar">
      <button v-for="t in tabs" :key="t.value"
        :class="['tab-btn', { active: filterStatus === t.value && filterDeleted === t.deleted }]"
        @click="filterStatus = t.value; filterDeleted = t.deleted; fetchMessages()">
        {{ t.label }}
        <span v-if="t.value === null" class="tab-count">{{ total }}</span>
      </button>
    </div>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>留言人</th><th>留言内容</th><th>留言时间</th>
            <th>审核状态</th><th>回复</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty-row">加载中...</td></tr>
          <tr v-else-if="!list.length"><td colspan="7" class="empty-row">暂无留言</td></tr>
          <tr v-for="row in list" :key="row.id" class="data-row">
            <td class="id-cell">#{{ row.id }}</td>
            <td>
              <div class="user-cell">
                <div class="mini-avatar">{{ (row.realName || row.userName || '?').charAt(0) }}</div>
                <div>
                  <div>{{ row.realName || row.userName }}</div>
                  <div class="sub-text">{{ row.userName }}</div>
                </div>
              </div>
            </td>
            <td class="content-cell">{{ row.content }}</td>
            <td class="time-cell">{{ formatTime(row.createTime) }}</td>
            <td>
              <span :class="['audit-badge', auditClass(row.auditStatus)]">
                {{ auditLabel(row.auditStatus) }}
              </span>
            </td>
            <td class="reply-cell">
              <span v-if="row.reply" class="has-reply">已回复</span>
              <span v-else class="no-reply">未回复</span>
            </td>
            <td>
              <div class="action-group">
                <template v-if="!filterDeleted">
                  <template v-if="row.auditStatus === 0">
                    <button class="action-btn approve" @click="handleApprove(row)">通过</button>
                    <button class="action-btn reject" @click="handleReject(row)">拒绝</button>
                  </template>
                  <button class="action-btn" @click="openReply(row)">回复</button>
                  <button class="action-btn danger" @click="handleDelete(row)">删除</button>
                </template>
                <span v-else class="deleted-label">已删除</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <span class="page-info">共 {{ total }} 条</span>
        <div class="page-btns">
          <button :disabled="page <= 1" @click="page--; fetchMessages()">上一页</button>
          <button :disabled="page >= totalPages" @click="page++; fetchMessages()">下一页</button>
        </div>
      </div>
    </div>

    <!-- 回复弹窗 -->
    <div v-if="showReply" class="modal-mask" @click.self="showReply = false">
      <div class="modal">
        <div class="modal-header">
          <h3>回复留言</h3>
          <button class="modal-close" @click="showReply = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="original-msg">
            <div class="original-label">原留言：</div>
            <div class="original-content">{{ replyTarget?.content }}</div>
          </div>
          <div class="form-group">
            <label>回复内容</label>
            <textarea v-model="replyContent" rows="4" placeholder="请输入回复内容..."></textarea>
          </div>
          <div v-if="replyError" class="form-error">{{ replyError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="showReply = false">取消</button>
          <button class="btn-primary" @click="submitReply">确认回复</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMessagesAdmin, approveMessage, rejectMessage, replyMessage, deleteMessage } from '@/api/admin'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = 10
const filterStatus = ref(null)
const filterDeleted = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const tabs = [
  { label: '全部', value: null, deleted: false },
  { label: '⏳ 待审核', value: 0, deleted: false },
  { label: '✅ 已通过', value: 1, deleted: false },
  { label: '❌ 已拒绝', value: 2, deleted: false },
  { label: '🗑 已删除', value: null, deleted: true },
]

const AUDIT_MAP = {
  0: { label: '待审核', cls: 'pending' },
  1: { label: '已通过', cls: 'approved' },
  2: { label: '已拒绝', cls: 'rejected' },
}
const auditLabel = s => AUDIT_MAP[s]?.label || s
const auditClass = s => AUDIT_MAP[s]?.cls || ''

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getMessagesAdmin({ 
      auditStatus: filterStatus.value, 
      deleted: filterDeleted.value ? 'true' : 'false', 
      page: page.value, 
      size 
    })
    list.value = res.data.data?.list || []
    total.value = res.data.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

async function handleApprove(row) {
  await approveMessage(row.id)
  row.auditStatus = 1
}

async function handleReject(row) {
  await rejectMessage(row.id)
  row.auditStatus = 2
}

async function handleDelete(row) {
  if (!confirm('确认删除该留言？')) return
  await deleteMessage(row.id)
  fetchMessages()
}

const showReply = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replyError = ref('')

function openReply(row) {
  replyTarget.value = row
  replyContent.value = row.reply || ''
  replyError.value = ''
  showReply.value = true
}

async function submitReply() {
  if (!replyContent.value.trim()) { replyError.value = '回复内容不能为空'; return }
  try {
    await replyMessage(replyTarget.value.id, replyContent.value)
    replyTarget.value.reply = replyContent.value
    showReply.value = false
  } catch (e) {
    replyError.value = e?.response?.data?.message || '回复失败'
  }
}

onMounted(fetchMessages)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 4px; font-size: 18px; color: #1a3a6b; }
.page-desc { margin: 0; font-size: 13px; color: #888; }

.tab-bar { display: flex; gap: 8px; }
.tab-btn { padding: 7px 18px; border-radius: 20px; border: 1px solid #d0d7de; background: #fff; font-size: 13px; cursor: pointer; color: #555; transition: all 0.15s; display: flex; align-items: center; gap: 6px; }
.tab-btn:hover { border-color: #2d6a9f; color: #2d6a9f; }
.tab-btn.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }
.tab-count { background: rgba(255,255,255,0.2); padding: 1px 6px; border-radius: 10px; font-size: 11px; }

.table-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); overflow: hidden; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th { background: #f8f9fb; padding: 11px 14px; text-align: left; color: #555; font-weight: 600; font-size: 12px; border-bottom: 1px solid #eee; }
td { padding: 12px 14px; border-bottom: 1px solid #f5f5f5; color: #333; vertical-align: top; }
.data-row:hover td { background: #fafbff; }
.data-row:last-child td { border-bottom: none; }
.empty-row { text-align: center; color: #bbb; padding: 40px; font-size: 14px; }
.id-cell { color: #aaa; font-size: 12px; }
.time-cell { color: #999; font-size: 12px; white-space: nowrap; }
.content-cell { max-width: 280px; line-height: 1.6; }
.sub-text { font-size: 11px; color: #aaa; }

.user-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.audit-badge { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.audit-badge.pending { background: #fff7e6; color: #d46b08; }
.audit-badge.approved { background: #f0fff4; color: #22863a; }
.audit-badge.rejected { background: #fff0f0; color: #cb2431; }

.has-reply { font-size: 12px; color: #389e0d; }
.no-reply { font-size: 12px; color: #aaa; }

.action-group { display: flex; gap: 6px; flex-wrap: wrap; }
.action-btn { background: none; border: 1px solid #d0d7de; border-radius: 5px; padding: 3px 10px; font-size: 12px; cursor: pointer; color: #2d6a9f; transition: all 0.15s; }
.action-btn:hover { background: #f0f4ff; border-color: #2d6a9f; }
.action-btn.approve { color: #389e0d; border-color: #b7eb8f; }
.action-btn.approve:hover { background: #f6ffed; }
.action-btn.reject { color: #cf1322; border-color: #ffccc7; }
.action-btn.reject:hover { background: #fff2f0; }
.action-btn.danger { color: #cf1322; border-color: #ffccc7; }
.action-btn.danger:hover { background: #fff2f0; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.page-info { font-size: 13px; color: #888; }
.page-btns { display: flex; gap: 8px; }
.page-btns button { padding: 4px 14px; border: 1px solid #d0d7de; border-radius: 5px; cursor: pointer; background: #fff; font-size: 13px; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 12px; width: 480px; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px 0; }
.modal-header h3 { margin: 0; font-size: 16px; color: #1a3a6b; }
.modal-close { background: none; border: none; font-size: 16px; cursor: pointer; color: #aaa; }
.modal-body { padding: 16px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 0 24px 20px; }
.original-msg { background: #f8f9fb; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.original-label { font-size: 12px; color: #aaa; margin-bottom: 6px; }
.original-content { font-size: 14px; color: #333; line-height: 1.6; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; color: #555; font-weight: 500; }
.form-group textarea { padding: 10px; border: 1px solid #d0d7de; border-radius: 7px; font-size: 13px; outline: none; resize: vertical; font-family: inherit; }
.form-group textarea:focus { border-color: #2d6a9f; }
.form-error { color: #cf1322; font-size: 13px; margin-top: 8px; }
.btn-primary { background: #1a3a6b; color: #fff; border: none; border-radius: 7px; padding: 7px 20px; font-size: 13px; cursor: pointer; }
.btn-primary:hover { background: #2d6a9f; }
.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 7px; padding: 6px 18px; font-size: 13px; cursor: pointer; }
.deleted-label { font-size: 12px; color: #bbb; font-style: italic; }
</style>
