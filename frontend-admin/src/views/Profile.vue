<template>
  <div class="profile">
    <el-card>
      <template #header>
        <span>个人信息</span>
      </template>
      <el-form :model="form" label-width="100px" style="max-width: 600px;">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input v-model="form.role" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()

const form = ref({
  username: '',
  role: '',
  email: '',
  phone: ''
})

const loadData = async () => {
  try {
    form.value = {
      username: userStore.username || '',
      role: userStore.role === 'ADMIN' ? '管理员' : userStore.role || '',
      email: userStore.email || '',
      phone: userStore.phone || ''
    }
  } catch (error) {
    console.error('加载个人信息失败', error)
    ElMessage.error('加载个人信息失败')
  }
}

const handleSubmit = async () => {
  try {
    // 这里可以添加更新用户信息的API调用
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存个人信息失败', error)
    ElMessage.error('保存个人信息失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.profile {
  padding: 0;
}

.profile :deep(.el-card) {
  border-radius: 16px;
}
</style>

