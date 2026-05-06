<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2>用户管理</h2>
        <p class="page-desc">管理系统中所有用户账号、角色分配及账号状态</p>
      </div>
      <button class="btn-primary" @click="openCreate">
        <span>＋</span> 新建用户
      </button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label>用户名</label>
          <input v-model="query.username" placeholder="请输入用户名" @keyup.enter="fetchUsers" />
        </div>
        <div class="filter-item">
          <label>角色</label>
          <select v-model="query.role">
            <option value="">全部角色</option>
            <option value="ROLE_CITIZEN">群众用户</option>
            <option value="ROLE_OFFICER">工作人员</option>
            <option value="ROLE_SUPER_ADMIN">超级管理员</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <select v-model="query.status">
            <option value="">全部状态</option>
            <option value="1">启用</option>
            <option value="0">禁用</option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn-primary" @click="fetchUsers">🔍 搜索</button>
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
            <th>用户ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>手机号</th>
            <th>所属部门</th>
            <th>账号状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="empty-row">
              <div class="loading-spin">⟳</div> 加载中...
            </td>
          </tr>
          <tr v-else-if="!list.length">
            <td colspan="8" class="empty-row">暂无用户数据</td>
          </tr>
          <tr v-for="row in list" :key="row.id" class="data-row">
            <td class="id-cell">#{{ row.id }}</td>
            <td>
              <div class="user-cell">
                <div class="mini-avatar">{{ (row.realName || row.username || '?').charAt(0) }}</div>
                <span>{{ row.username }}</span>
              </div>
            </td>
            <td>{{ row.realName || '-' }}</td>
            <td class="phone-cell">{{ row.phone }}</td>
            <td>{{ row.deptName || '未分配' }}</td>
            <td>
              <span :class="['status-badge', row.status === 1 ? 'enabled' : 'disabled']">
                {{ row.status === 1 ? '● 启用' : '● 禁用' }}
              </span>
            </td>
            <td class="time-cell">{{ row.createTime }}</td>
            <td>
              <div class="action-group">
                <button class="action-btn" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </button>
                <button class="action-btn" @click="openChangeRole(row)">修改角色</button>
                <button class="action-btn danger" @click="openResetPwd(row)">重置密码</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination">
        <span class="page-info">第 {{ query.page }} / {{ totalPages }} 页</span>
        <div class="page-btns">
          <button :disabled="query.page <= 1" @click="query.page--; fetchUsers()">‹ 上一页</button>
          <button
            v-for="p in pageRange" :key="p"
            :class="['page-num', { active: p === query.page }]"
            @click="query.page = p; fetchUsers()"
          >{{ p }}</button>
          <button :disabled="query.page >= totalPages" @click="query.page++; fetchUsers()">下一页 ›</button>
        </div>
      </div>
    </div>

    <!-- 新建用户弹窗 -->
    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-header">
          <h3>新建用户</h3>
          <button class="modal-close" @click="showCreate = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>用户名 <span class="required">*</span></label>
              <input v-model="form.username" placeholder="请输入用户名" />
            </div>
            <div class="form-group">
              <label>真实姓名</label>
              <input v-model="form.realName" placeholder="请输入真实姓名" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>密码 <span class="required">*</span></label>
              <input v-model="form.password" type="password" placeholder="至少8位，含字母和数字" />
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input v-model="form.phone" placeholder="请输入手机号" />
            </div>
          </div>
          <div class="form-group">
            <label>分配角色 <span class="required">*</span></label>
            <select v-model="form.roleId">
              <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}（{{ r.roleCode }}）</option>
            </select>
          </div>
          <div v-if="formError" class="form-error">⚠ {{ formError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="showCreate = false">取消</button>
          <button class="btn-primary" @click="submitCreate">确认创建</button>
        </div>
      </div>
    </div>

    <!-- 修改角色弹窗 -->
    <div v-if="showRole" class="modal-mask" @click.self="showRole = false">
      <div class="modal modal-sm">
        <div class="modal-header">
          <h3>修改角色</h3>
          <button class="modal-close" @click="showRole = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="role-target">
            为用户 <b>{{ roleTarget?.username }}</b>（{{ roleTarget?.realName }}）修改角色
          </div>
          <div class="form-group">
            <label>选择角色 <span class="required">*</span></label>
            <div class="role-options">
              <label v-for="r in roles" :key="r.id" class="role-option">
                <input type="radio" :value="r.id" v-model="selectedRoleId" />
                <span class="role-option-name">{{ r.roleName }}</span>
                <span class="role-option-code">{{ r.roleCode }}</span>
              </label>
            </div>
          </div>
          <div v-if="roleError" class="form-error">{{ roleError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="showRole = false">取消</button>
          <button class="btn-primary" @click="submitRole">确认修改</button>
        </div>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div v-if="showReset" class="modal-mask" @click.self="showReset = false">
      <div class="modal modal-sm">
        <div class="modal-header">
          <h3>重置密码</h3>
          <button class="modal-close" @click="showReset = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="reset-target">
            正在为用户 <b>{{ resetTarget?.username }}</b>（{{ resetTarget?.realName }}）重置密码
          </div>
          <div class="form-group">
            <label>新密码 <span class="required">*</span></label>
            <input v-model="newPwd" type="password" placeholder="至少8位，含字母和数字" />
          </div>
          <div v-if="resetError" class="form-error">⚠ {{ resetError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="showReset = false">取消</button>
          <button class="btn-primary" @click="submitReset">确认重置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUsers, createUser, updateUserStatus, resetUserPassword, getRoles, updateUserRoles } from '@/api/admin'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])

const query = reactive({ username: '', role: '', status: '', page: 1, size: 10 })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const pageRange = computed(() => {
  const pages = []
  const start = Math.max(1, query.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers({ username: query.username, role: query.role, status: query.status, page: query.page, size: query.size })
    list.value = res.data.data?.list || []
    total.value = res.data.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function resetQuery() {
  Object.assign(query, { username: '', role: '', status: '', page: 1 })
  fetchUsers()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, newStatus)
    row.status = newStatus
  } catch (e) {
    alert(e?.response?.data?.message || '操作失败')
  }
}

const showCreate = ref(false)
const form = reactive({ username: '', password: '', realName: '', phone: '', roleId: '' })
const formError = ref('')

function openCreate() {
  Object.assign(form, { username: '', password: '', realName: '', phone: '', roleId: roles.value[0]?.id || '' })
  formError.value = ''
  showCreate.value = true
}

async function submitCreate() {
  formError.value = ''
  try {
    await createUser({ ...form, roleIds: [Number(form.roleId)] })
    showCreate.value = false
    fetchUsers()
  } catch (e) {
    formError.value = e?.response?.data?.message || '创建失败，请检查输入信息'
  }
}

const showReset = ref(false)
const resetTarget = ref(null)
const newPwd = ref('')
const resetError = ref('')

function openResetPwd(row) {
  resetTarget.value = row
  newPwd.value = ''
  resetError.value = ''
  showReset.value = true
}

async function submitReset() {
  resetError.value = ''
  try {
    await resetUserPassword(resetTarget.value.id, newPwd.value)
    showReset.value = false
  } catch (e) {
    resetError.value = e?.response?.data?.message || '重置失败'
  }
}

// 修改角色
const showRole = ref(false)
const roleTarget = ref(null)
const selectedRoleId = ref(null)
const roleError = ref('')

function openChangeRole(row) {
  roleTarget.value = row
  // 默认选中当前第一个角色
  const currentRole = roles.value.find(r => row.roles?.includes(r.roleCode))
  selectedRoleId.value = currentRole?.id || roles.value[0]?.id || null
  roleError.value = ''
  showRole.value = true
}

async function submitRole() {
  if (!selectedRoleId.value) { roleError.value = '请选择角色'; return }
  roleError.value = ''
  try {
    await updateUserRoles(roleTarget.value.id, [Number(selectedRoleId.value)])
    showRole.value = false
    fetchUsers()
  } catch (e) {
    roleError.value = e?.response?.data?.message || '修改失败'
  }
}

onMounted(async () => {
  const res = await getRoles()
  roles.value = res.data.data || []
  fetchUsers()
})
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
.filter-item input, .filter-item select {
  height: 34px; padding: 0 10px; border: 1px solid #d0d7de;
  border-radius: 6px; font-size: 13px; outline: none; min-width: 140px;
}
.filter-item input:focus, .filter-item select:focus { border-color: #2d6a9f; }
.filter-actions { display: flex; gap: 8px; padding-bottom: 0; }

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
.loading-spin { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.id-cell { color: #aaa; font-size: 12px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, #1a3a6b, #2d6a9f);
  color: #fff; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.phone-cell { color: #666; font-family: monospace; }
.time-cell { color: #999; font-size: 12px; }

.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-badge.enabled { background: #f0fff4; color: #22863a; }
.status-badge.disabled { background: #fff0f0; color: #cb2431; }

.action-group { display: flex; gap: 8px; }
.action-btn {
  background: none; border: 1px solid #d0d7de; border-radius: 5px;
  padding: 3px 10px; font-size: 12px; cursor: pointer; color: #2d6a9f;
  transition: all 0.15s;
}
.action-btn:hover { background: #f0f4ff; border-color: #2d6a9f; }
.action-btn.danger { color: #cf1322; border-color: #ffccc7; }
.action-btn.danger:hover { background: #fff2f0; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.page-info { font-size: 13px; color: #888; }
.page-btns { display: flex; gap: 4px; }
.page-btns button { padding: 4px 10px; border: 1px solid #d0d7de; border-radius: 5px; cursor: pointer; background: #fff; font-size: 13px; color: #555; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btns button:hover:not(:disabled) { border-color: #2d6a9f; color: #2d6a9f; }
.page-num.active { background: #1a3a6b; color: #fff; border-color: #1a3a6b; }

.btn-primary { background: #1a3a6b; color: #fff; border: none; border-radius: 7px; padding: 7px 16px; font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 5px; }
.btn-primary:hover { background: #2d6a9f; }
.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 7px; padding: 6px 14px; font-size: 13px; cursor: pointer; color: #555; }
.btn-default:hover { background: #f5f5f5; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 12px; width: 520px; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }
.modal-sm { width: 400px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px 0; }
.modal-header h3 { margin: 0; font-size: 16px; color: #1a3a6b; }
.modal-close { background: none; border: none; font-size: 16px; cursor: pointer; color: #aaa; padding: 0; }
.modal-close:hover { color: #333; }
.modal-body { padding: 16px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 0 24px 20px; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: 13px; color: #555; font-weight: 500; }
.form-group input, .form-group select {
  height: 36px; padding: 0 10px; border: 1px solid #d0d7de;
  border-radius: 7px; font-size: 13px; outline: none;
}
.form-group input:focus, .form-group select:focus { border-color: #2d6a9f; box-shadow: 0 0 0 3px rgba(45,106,159,0.1); }
.required { color: #cf1322; }
.form-error { background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; padding: 8px 12px; font-size: 13px; color: #cf1322; margin-bottom: 8px; }
.reset-target { background: #f0f4ff; border-radius: 6px; padding: 10px 12px; font-size: 13px; color: #333; margin-bottom: 14px; }
.role-target { background: #f0f4ff; border-radius: 6px; padding: 10px 12px; font-size: 13px; color: #333; margin-bottom: 14px; }
.role-options { display: flex; flex-direction: column; gap: 10px; }
.role-option { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border: 1px solid #e0e0e0; border-radius: 8px; cursor: pointer; transition: all 0.15s; }
.role-option:has(input:checked) { border-color: #1a3a6b; background: #f0f4ff; }
.role-option input { accent-color: #1a3a6b; }
.role-option-name { font-size: 14px; font-weight: 500; color: #333; flex: 1; }
.role-option-code { font-size: 12px; color: #aaa; font-family: monospace; }
</style>
