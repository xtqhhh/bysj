<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>审计日志</h2>
        <p class="page-desc">记录系统所有关键操作，支持安全审计与问题追溯</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label>操作人ID</label>
          <input v-model="query.operatorId" placeholder="请输入操作人ID" type="number" />
        </div>
        <div class="filter-item">
          <label>操作类型</label>
          <select v-model="query.type">
            <option value="">全部类型</option>
            <option value="LOGIN">登录</option>
            <option value="LOGOUT">登出</option>
            <option value="LOGIN_FAIL">登录失败</option>
            <option value="PASSWORD_CHANGE">密码修改</option>
            <option value="ROLE_CHANGE">角色变更</option>
            <option value="PERMISSION_CHANGE">权限变更</option>
            <option value="USER_DISABLE">用户禁用</option>
            <option value="USER_ENABLE">用户启用</option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input v-model="query.startTime" type="datetime-local" />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input v-model="query.endTime" type="datetime-local" />
        </div>
        <div class="filter-actions">
          <button class="btn-primary" @click="fetchLogs">🔍 搜索</button>
          <button class="btn-default" @click="resetQuery">重置</button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-total">共 <b>{{ total }}</b> 条记录</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>日志ID</th>
            <th>操作人</th>
            <th>操作类型</th>
            <th>客户端IP</th>
            <th>操作时间</th>
            <th>操作结果</th>
            <th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="empty-row">加载中...</td>
          </tr>
          <tr v-else-if="!list.length">
            <td colspan="7" class="empty-row">暂无审计日志</td>
          </tr>
          <tr v-for="row in list" :key="row.id" class="data-row">
            <td class="id-cell">#{{ row.id }}</td>
            <td>
              <div class="operator-cell">
                <div class="mini-avatar">{{ (row.operatorName || '?').charAt(0) }}</div>
                <div>
                  <div class="op-name">{{ row.operatorName }}</div>
                  <div class="op-id">ID: {{ row.operatorId }}</div>
                </div>
              </div>
            </td>
            <td>
              <span :class="['type-badge', typeClass(row.operationType)]">
                {{ typeLabel(row.operationType) }}
              </span>
            </td>
            <td class="ip-cell">{{ row.requestIp }}</td>
            <td class="time-cell">{{ row.operationTime }}</td>
            <td>
              <span :class="['result-badge', row.result === 'SUCCESS' ? 'success' : 'fail']">
                {{ row.result === 'SUCCESS' ? '✓ 成功' : '✗ 失败' }}
              </span>
            </td>
            <td>
              <button class="detail-btn" @click="viewDetail(row)">查看详情</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="page-info">第 {{ query.page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <div class="page-btns">
          <button :disabled="query.page <= 1" @click="query.page--; fetchLogs()">‹ 上一页</button>
          <button
            v-for="p in pageRange" :key="p"
            :class="['page-num', { active: p === query.page }]"
            @click="query.page = p; fetchLogs()"
          >{{ p }}</button>
          <button :disabled="query.page >= totalPages" @click="query.page++; fetchLogs()">下一页 ›</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal">
        <div class="modal-header">
          <h3>操作详情</h3>
          <button class="modal-close" @click="detail = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">操作人</span>
              <span class="detail-value">{{ detail.operatorName }}（ID: {{ detail.operatorId }}）</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作类型</span>
              <span class="detail-value">
                <span :class="['type-badge', typeClass(detail.operationType)]">{{ typeLabel(detail.operationType) }}</span>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">客户端IP</span>
              <span class="detail-value ip-cell">{{ detail.requestIp }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作时间</span>
              <span class="detail-value">{{ detail.operationTime }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作结果</span>
              <span :class="['result-badge', detail.result === 'SUCCESS' ? 'success' : 'fail']">
                {{ detail.result === 'SUCCESS' ? '✓ 成功' : '✗ 失败' }}
              </span>
            </div>
          </div>
          <div v-if="detail.beforeData" class="diff-section">
            <div class="diff-label">变更前数据</div>
            <pre class="diff-pre before">{{ formatJson(detail.beforeData) }}</pre>
          </div>
          <div v-if="detail.afterData" class="diff-section">
            <div class="diff-label">变更后数据</div>
            <pre class="diff-pre after">{{ formatJson(detail.afterData) }}</pre>
          </div>
          <div v-if="!detail.beforeData && !detail.afterData" class="no-diff">
            此操作无数据变更记录
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="detail = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getAuditLogs } from '@/api/admin'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const detail = ref(null)

const query = reactive({ operatorId: '', type: '', startTime: '', endTime: '', page: 1, size: 15 })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const pageRange = computed(() => {
  const pages = []
  const start = Math.max(1, query.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs({ operatorId: query.operatorId, type: query.type, startTime: query.startTime, endTime: query.endTime, page: query.page, size: query.size })
    list.value = res.data.data?.list || []
    total.value = res.data.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function resetQuery() {
  Object.assign(query, { operatorId: '', type: '', startTime: '', endTime: '', page: 1 })
  fetchLogs()
}

function viewDetail(row) { detail.value = row }
function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

const TYPE_MAP = {
  LOGIN: { label: '登录', cls: 'type-login' },
  LOGOUT: { label: '登出', cls: 'type-logout' },
  LOGIN_FAIL: { label: '登录失败', cls: 'type-fail' },
  PASSWORD_CHANGE: { label: '密码修改', cls: 'type-pwd' },
  ROLE_CHANGE: { label: '角色变更', cls: 'type-role' },
  PERMISSION_CHANGE: { label: '权限变更', cls: 'type-role' },
  USER_DISABLE: { label: '用户禁用', cls: 'type-disable' },
  USER_ENABLE: { label: '用户启用', cls: 'type-enable' },
}
const typeLabel = t => TYPE_MAP[t]?.label || t
const typeClass = t => TYPE_MAP[t]?.cls || ''

onMounted(fetchLogs)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 4px; font-size: 18px; color: #1a3a6b; }
.page-desc { margin: 0; font-size: 13px; color: #888; }

.filter-card { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.filter-row { display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap; }
.filter-item { display: flex; flex-direction: column; gap: 5px; }
.filter-item label { font-size: 12px; color: #666; }
.filter-item input, .filter-item select { height: 34px; padding: 0 10px; border: 1px solid #d0d7de; border-radius: 6px; font-size: 13px; outline: none; min-width: 140px; }
.filter-item input:focus, .filter-item select:focus { border-color: #2d6a9f; }
.filter-actions { display: flex; gap: 8px; }

.table-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); overflow: hidden; }
.table-toolbar { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
.table-total { font-size: 13px; color: #666; }
.table-total b { color: #1a3a6b; }

table { width: 100%; border-collapse: collapse; font-size: 13px; }
th { background: #f8f9fb; padding: 11px 14px; text-align: left; color: #555; font-weight: 600; font-size: 12px; border-bottom: 1px solid #eee; }
td { padding: 12px 14px; border-bottom: 1px solid #f5f5f5; color: #333; }
.data-row:hover td { background: #fafbff; }
.data-row:last-child td { border-bottom: none; }
.empty-row { text-align: center; color: #bbb; padding: 40px; font-size: 14px; }
.id-cell { color: #aaa; font-size: 12px; }
.ip-cell { font-family: monospace; font-size: 12px; color: #666; }
.time-cell { color: #999; font-size: 12px; }

.operator-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #1a3a6b, #2d6a9f); color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.op-name { font-size: 13px; font-weight: 500; }
.op-id { font-size: 11px; color: #aaa; }

.type-badge { padding: 3px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.type-login { background: #e6f4ff; color: #0958d9; }
.type-logout { background: #f0f0f0; color: #555; }
.type-fail { background: #fff2f0; color: #cf1322; }
.type-pwd { background: #fff7e6; color: #d46b08; }
.type-role { background: #f9f0ff; color: #531dab; }
.type-disable { background: #fff2f0; color: #cf1322; }
.type-enable { background: #f6ffed; color: #389e0d; }

.result-badge { padding: 3px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.result-badge.success { background: #f0fff4; color: #22863a; }
.result-badge.fail { background: #fff0f0; color: #cb2431; }

.detail-btn { background: none; border: 1px solid #d0d7de; border-radius: 5px; padding: 3px 10px; font-size: 12px; cursor: pointer; color: #2d6a9f; }
.detail-btn:hover { background: #f0f4ff; border-color: #2d6a9f; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.page-info { font-size: 13px; color: #888; }
.page-btns { display: flex; gap: 4px; }
.page-btns button { padding: 4px 10px; border: 1px solid #d0d7de; border-radius: 5px; cursor: pointer; background: #fff; font-size: 13px; color: #555; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btns button:hover:not(:disabled) { border-color: #2d6a9f; color: #2d6a9f; }
.page-num.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }

.btn-primary { background: #1a3a6b; color: #fff; border: none; border-radius: 7px; padding: 7px 16px; font-size: 13px; cursor: pointer; }
.btn-primary:hover { background: #2d6a9f; }
.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 7px; padding: 6px 14px; font-size: 13px; cursor: pointer; color: #555; }
.btn-default:hover { background: #f5f5f5; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 12px; width: 580px; max-height: 85vh; overflow-y: auto; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px 0; position: sticky; top: 0; background: #fff; }
.modal-header h3 { margin: 0; font-size: 16px; color: #1a3a6b; }
.modal-close { background: none; border: none; font-size: 16px; cursor: pointer; color: #aaa; }
.modal-close:hover { color: #333; }
.modal-body { padding: 16px 24px; }
.modal-footer { display: flex; justify-content: flex-end; padding: 0 24px 20px; position: sticky; bottom: 0; background: #fff; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 11px; color: #aaa; text-transform: uppercase; letter-spacing: 0.5px; }
.detail-value { font-size: 13px; color: #333; font-weight: 500; }

.diff-section { margin-bottom: 12px; }
.diff-label { font-size: 12px; font-weight: 600; color: #555; margin-bottom: 6px; }
.diff-pre { background: #f8f9fb; border: 1px solid #e8e8e8; border-radius: 6px; padding: 12px; font-size: 12px; overflow-x: auto; margin: 0; line-height: 1.6; }
.diff-pre.before { border-left: 3px solid #ffccc7; }
.diff-pre.after { border-left: 3px solid #b7eb8f; }
.no-diff { color: #aaa; font-size: 13px; text-align: center; padding: 16px 0; }
</style>
