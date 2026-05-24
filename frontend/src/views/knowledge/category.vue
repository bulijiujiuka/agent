<template>
  <div class="category-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识库分类管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="分类名称" width="180" />
        <el-table-column prop="description" label="描述" min-width="250" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：公司制度" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="分类描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/kbCategory'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const defaultForm = () => ({ id: null, name: '', description: '', sortOrder: 0 })
const form = ref(defaultForm())
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    tableData.value = res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增分类'; form.value = defaultForm(); dialogVisible.value = true }
const handleEdit = (row) => { dialogTitle.value = '编辑分类'; form.value = { ...row }; dialogVisible.value = true }
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteCategory(row.id); ElMessage.success('删除成功'); fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.value.id) { await updateCategory(form.value); ElMessage.success('更新成功') }
      else { await createCategory(form.value); ElMessage.success('创建成功') }
      dialogVisible.value = false; fetchData()
    }
  })
}

onMounted(() => { fetchData() })
</script>

<style lang="scss" scoped>
.category-container {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
}
</style>
