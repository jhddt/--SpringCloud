<template>
  <div class="profile">
    <el-card>
      <template #header>
        <span>个人信息</span>
      </template>
      <el-form :model="form" label-width="100px" style="max-width: 600px;">
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :action="'/api/file/upload/avatar'"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.avatar" :src="form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()

const form = ref({
  username: '',
  role: '',
  email: '',
  phone: '',
  avatar: ''
})

const loadData = async () => {
  try {
    form.value = {
      username: userStore.username || '',
      role: userStore.role === 'ADMIN' ? '管理员' : userStore.role || '',
      email: userStore.email || '',
      phone: userStore.phone || '',
      avatar: userStore.avatar || ''
    }
  } catch (error) {
    console.error('加载个人信息失败', error)
    ElMessage.error('加载个人信息失败')
  }
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    const avatarUrl = response.data.url
    form.value.avatar = avatarUrl
    // 更新store中的头像
    userStore.avatar = avatarUrl
    sessionStorage.setItem('avatar', avatarUrl)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const handleAvatarError = (error) => {
  console.error('头像上传错误:', error)
  const errorMessage = error?.response?.data?.message || error?.message || '头像上传失败，请稍后重试'
  ElMessage.error(errorMessage)
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const handleSubmit = async () => {
  try {
    // 这里可以添加更新用户信息的API调用
    // 如果头像已更改，更新store
    if (form.value.avatar) {
      userStore.avatar = form.value.avatar
      sessionStorage.setItem('avatar', form.value.avatar)
    }
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

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}
</style>

