<template>
  <div class="sensitive-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>敏感词管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增敏感词</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="word" label="敏感词" width="160" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            {{ categoryLabel(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="replacement" label="替换文本" width="120" />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
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
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" placeholder="输入敏感词" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width:100%" placeholder="选择分类" allow-create filterable>
            <el-option label="暴力" value="VIOLENCE" />
            <el-option label="赌博" value="GAMBLING" />
            <el-option label="色情" value="PORN" />
            <el-option label="毒品" value="DRUG" />
            <el-option label="武器" value="WEAPON" />
            <el-option label="自残" value="HARM" />
            <el-option label="欺诈" value="FRAUD" />
            <el-option label="默认" value="DEFAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="替换文本">
          <el-input v-model="form.replacement" placeholder="默认 ***" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
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
import { getSensitiveWordList, createSensitiveWord, updateSensitiveWord, deleteSensitiveWord } from '@/api/sensitiveWord'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const defaultForm = () => ({ id: null, word: '', category: 'DEFAULT', replacement: '***', enabled: true })
const form = ref(defaultForm())

const categoryLabelMap = { VIOLENCE: '暴力', GAMBLING: '赌博', PORN: '色情', DRUG: '毒品', WEAPON: '武器', HARM: '自残', FRAUD: '欺诈', DEFAULT: '默认' }
const categoryLabel = (val) => categoryLabelMap[val] || val

const rules = { word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSensitiveWordList()
    tableData.value = res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增敏感词'; form.value = defaultForm(); dialogVisible.value = true }
const handleEdit = (row) => { dialogTitle.value = '编辑敏感词'; form.value = { ...row }; dialogVisible.value = true }
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.word}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteSensitiveWord(row.id); ElMessage.success('删除成功'); fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.value.id) { await updateSensitiveWord(form.value); ElMessage.success('更新成功') }
      else { await createSensitiveWord(form.value); ElMessage.success('创建成功') }
      dialogVisible.value = false; fetchData()
    }
  })
}

onMounted(() => { fetchData() })
</script>

<style lang="scss" scoped>
.sensitive-container {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
}
</style>
