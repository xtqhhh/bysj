<template>
  <div class="page">
    <div class="page-header">
      <h2>角色管理</h2>
      <button class="btn-primary" @click="openCreate">+ 新建角色</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>角色名称</th><th>角色编码</th><th>数据权限</th><th>创建时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="center">加载中...</td></tr>
          <tr v-else-if="!list.length"><td colspan="6" class="center">暂无数据</td></tr>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.roleName }}</td>
            <td><code>{{ row.roleCode }}</code></td>
            <td>{{ dataScopeLabel(row.dataScope) }}</td>
            <td>{{ row.createTime }}</td>
            <td class="actions">
              <button class="btn-link" @click="openEdit(row)">编辑</button>
              <button class="btn-link" @click="openPermissions(row)">权限分配</button>
              <button class="btn-link danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
      <div class="modal">
        <h3>{{ editTarget ? '编辑角色' : '新建角色' }}</h3>
        <div class="form-group">
          <label>角色名称</label>
          <input v-model="form.roleName" placeholder="如：工作人员" />
        </div>
        <div class="form-group">
          <label>角色编码</label>
          <input v-model="form.roleCode" placeholder="如：ROLE_OFFICER" :disabled="!!editTarget" />
        </div>
        <div class="form-group">
          <label>数据权限</label>
          <select v-model="form.dataScope">
            <option value="ALL">全部数据</option>
            <option value="DEPT">本部门数据</option>
            <option value="SELF">本人数据</option>
          </select>
        </div>
        <div v-if="formError" class="error-msg">{{ formError }}</div>
        <div class="modal-footer">
          <button class="btn-default" @click="showForm = false">取消</button>
          <button class="btn-primary" @click="submitForm">确认</button>
        </div>
      </div>
    </div>

    <!-- 权限分配弹窗（简化版） -->
    <div v-if="showPerms" class="modal-mask" @click.self="showPerms = false">
      <div class="modal">
        <h3>权限分配 — {{ permTarget?.roleName }}</h3>
        <p style="font-size:13px;color:#888;margin:0 0 16px">
          权限分配功能需结合后端权限列表接口实现，此处为占位展示。
        </p>
        <div class="modal-footer">
          <button class="btn-default" @click="showPerms = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRoles, createRole, updateRole, deleteRole } from '@/api/admin'

const list = ref([])
const loading = ref(false)

const SCOPE_LABELS = { ALL: '全部数据', DEPT: '本部门', SELF: '本人' }
const dataScopeLabel = s => SCOPE_LABELS[s] || s

async function fetchRoles() {
  loading.value = true
  try {
    const res = await getRoles()
    list.value = res.data.data || []
  } catch { list.value = [] } finally { loading.value = false }
}

// 新建/编辑
const showForm = ref(false)
const editTarget = ref(null)
const form = reactive({ roleName: '', roleCode: '', dataScope: 'SELF' })
const formError = ref('')

function openCreate() {
  editTarget.value = null
  Object.assign(form, { roleName: '', roleCode: '', dataScope: 'SELF' })
  formError.value = ''
  showForm.value = true
}

function openEdit(row) {
  editTarget.value = row
  Object.assign(form, { roleName: row.roleName, roleCode: row.roleCode, dataScope: row.dataScope })
  formError.value = ''
  showForm.value = true
}

async function submitForm() {
  formError.value = ''
  try {
    if (editTarget.value) {
      await updateRole(editTarget.value.id, { roleName: form.roleName, dataScope: form.dataScope })
    } else {
      await createRole({ ...form })
    }
    showForm.value = false
    fetchRoles()
  } catch (e) {
    formError.value = e?.response?.data?.message || '操作失败'
  }
}

async function handleDelete(row) {
  if (!confirm(`确认删除角色「${row.roleName}」？`)) return
  try {
    await deleteRole(row.id)
    fetchRoles()
  } catch (e) {
    alert(e?.response?.data?.message || '删除失败')
  }
}

// 权限分配
const showPerms = ref(false)
const permTarget = ref(null)
function openPermissions(row) {
  permTarget.value = row
  showPerms.value = true
}

onMounted(fetchRoles)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { margin: 0; font-size: 18px; color: #1a3a6b; }
.table-wrap { background: #fff; border-radius: 8px; overflow: auto; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th { background: #f5f7fa; padding: 10px 12px; text-align: left; color: #555; font-weight: 600; }
td { padding: 10px 12px; border-top: 1px solid #f0f0f0; color: #333; }
.center { text-align: center; color: #aaa; }
code { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.actions { display: flex; gap: 8px; }
.btn-link { background: none; border: none; cursor: pointer; font-size: 13px; color: #2d6a9f; padding: 0; }
.btn-link.danger { color: #cf1322; }
.btn-link:hover { text-decoration: underline; }
.btn-primary { background: #1a3a6b; color: #fff; border: none; border-radius: 6px; padding: 7px 16px; font-size: 13px; cursor: pointer; }
.btn-primary:hover { background: #2d6a9f; }
.btn-default { background: #fff; border: 1px solid #d0d7de; border-radius: 6px; padding: 6px 14px; font-size: 13px; cursor: pointer; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 8px; padding: 28px; width: 400px; }
.modal h3 { margin: 0 0 20px; font-size: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: 13px; color: #555; }
.form-group input, .form-group select { height: 36px; padding: 0 10px; border: 1px solid #d0d7de; border-radius: 6px; font-size: 13px; outline: none; }
.error-msg { color: #cf1322; font-size: 13px; margin-bottom: 10px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
</style>
