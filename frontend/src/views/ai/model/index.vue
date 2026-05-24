<template>
  <div class="model-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI 模型管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增模型</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="modelKey" label="模型标识" width="140" />
        <el-table-column prop="modelName" label="显示名称" width="140" />
        <el-table-column prop="provider" label="提供商" width="100" />
        <el-table-column prop="baseUrl" label="API 地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="modelId" label="模型ID" width="150" />
        <el-table-column prop="temperature" label="温度" width="70" align="center" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="100" align="center" />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="warning" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型标识" prop="modelKey">
          <el-input v-model="form.modelKey" placeholder="如 deepseek-v3" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="显示名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 DeepSeek V3" />
        </el-form-item>
        <el-form-item label="提供商">
          <el-select v-model="form.provider" style="width:100%">
            <el-option label="OpenAI 兼容" value="OPENAI" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 地址" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-xxx" />
        </el-form-item>
        <el-form-item label="模型 ID" prop="modelId">
          <el-input v-model="form.modelId" placeholder="如 gpt-4o, deepseek-v3.2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="温度">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Max Tokens">
              <el-input-number v-model="form.maxTokens" :min="256" :max="32768" :step="256" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为默认">
              <el-switch v-model="form.isDefault" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { getModelConfigList, createModelConfig, updateModelConfig, deleteModelConfig } from '@/api/modelConfig'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const defaultForm = () => ({
  id: null, modelKey: '', modelName: '', provider: 'OPENAI', baseUrl: '', apiKey: '', modelId: '',
  temperature: 0.7, maxTokens: 2048, enabled: true, isDefault: false, sortOrder: 0
})
const form = ref(defaultForm())

const rules = {
  modelKey: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
  modelId: [{ required: true, message: '请输入模型 ID', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getModelConfigList()
    tableData.value = res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增模型'; form.value = defaultForm(); dialogVisible.value = true }
const handleEdit = (row) => { dialogTitle.value = '编辑模型'; form.value = { ...row }; dialogVisible.value = true }
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.modelName}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteModelConfig(row.id); ElMessage.success('删除成功'); fetchData()
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.value.id) { await updateModelConfig(form.value); ElMessage.success('更新成功') }
      else { await createModelConfig(form.value); ElMessage.success('创建成功') }
      dialogVisible.value = false; fetchData()
    }
  })
}

onMounted(() => { fetchData() })
</script>

<style lang="scss" scoped>
.model-container {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
}
</style>
