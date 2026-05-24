<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-button type="success" :icon="Download" @click="handleExport">导出Excel</el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="name in (row.roleNames || [])" :key="name" size="small" style="margin-right: 4px;">{{ name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-tabs v-model="editTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
            </el-form-item>
            <template v-if="!form.id">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
              </el-form-item>
            </template>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <div style="text-align: right;">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">保存</el-button>
          </div>
        </el-tab-pane>

        <!-- 修改密码（仅编辑时） -->
        <el-tab-pane v-if="form.id" label="修改密码" name="password">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
          </el-form>
          <div style="text-align: right;">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="pwdLoading" @click="handleResetPassword">重置密码</el-button>
          </div>
        </el-tab-pane>

        <!-- 分配角色（仅编辑时） -->
        <el-tab-pane v-if="form.id" label="分配角色" name="role">
          <el-checkbox-group v-model="selectedRoleIds">
            <el-checkbox v-for="role in roleList" :key="role.id" :label="role.id" style="display: block; margin-bottom: 8px;">
              {{ role.roleName }}
              <span style="color: #909399; font-size: 12px; margin-left: 8px;">{{ role.description }}</span>
            </el-checkbox>
          </el-checkbox-group>
          <div style="text-align: right; margin-top: 16px;">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="roleLoading" @click="handleSubmitRoles">保存角色</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus, Search, Refresh, Edit, Delete, UserFilled, Download } from '@element-plus/icons-vue'
import { getUserPage, createUser, updateUser, deleteUser, getUserRoles, assignUserRoles, resetUserPassword } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { getRoleList } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const searchForm = reactive({
  username: '',
  status: ''
})

const editTab = ref('info')

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      status: searchForm.status !== '' && searchForm.status !== null ? searchForm.status : undefined
    }
    const res = await getUserPage(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.status = ''
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  editTab.value = 'info'
  Object.assign(form, { id: null, username: '', password: '', nickname: '', email: '', phone: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑用户'
  editTab.value = 'info'
  Object.assign(form, { ...row, password: '' })
  // 预加载角色数据
  try {
    const [rolesRes, userRolesRes] = await Promise.all([
      getRoleList(),
      getUserRoles(row.id)
    ])
    roleList.value = rolesRes.data || []
    selectedRoleIds.value = userRolesRes.data || []
  } catch (e) {
    console.error(e)
  }
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        await updateUser(form)
        ElMessage.success('更新成功')
      } else {
        await createUser(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    }
  })
}

// 分配角色
const roleLoading = ref(false)
const selectedRoleIds = ref([])
const roleList = ref([])

const handleSubmitRoles = async () => {
  roleLoading.value = true
  try {
    await assignUserRoles(form.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    fetchData()
  } catch (error) {
    console.error(error)
  } finally {
    roleLoading.value = false
  }
}

// 修改密码
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ newPassword: '', confirmPassword: '' })
const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleResetPassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await resetUserPassword(form.id, pwdForm.newPassword)
      ElMessage.success('密码重置成功')
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } catch (error) {
      console.error(error)
    } finally {
      pwdLoading.value = false
    }
  })
}

const handleExport = () => {
  const userStore = useUserStore()
  const link = document.createElement('a')
  link.href = '/api/user/export'
  // 通过 fetch 带 token 下载
  fetch('/api/user/export', {
    headers: { Authorization: `Bearer ${userStore.token}` }
  }).then(res => res.blob()).then(blob => {
    const url = URL.createObjectURL(blob)
    link.href = url
    link.download = '用户列表.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  })
}

watch([pageNum, pageSize], () => {
  fetchData()
})

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.user-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
