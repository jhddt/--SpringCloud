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
        <el-form-item label="工号">
          <el-input v-model="form.teacherNo" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
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
  id: null,
  teacherNo: '',
  realName: '',
  department: '',
  title: '',
  phone: '',
  email: '',
  avatar: ''
})

const loadData = async () => {
  try {
    // 使用getByUserId接口获取教师信息
    const response = await api.get(`/teacher/user/${userStore.userId}`)
    if (response.data.code === 200 && response.data.data) {
      const data = response.data.data
      form.value = {
        id: data.teacherId || data.id,
        teacherNo: data.username || '',
        realName: data.name || '',
        department: data.department || '',
        title: data.title || '',
        phone: data.phone || '',
        email: data.email || '',
        avatar: data.avatarUrl || ''
      }
    }
  } catch (error) {
    if (error.response?.status === 404 || error.response?.data?.code === 404) {
      // 教师记录不存在，这是正常的，不需要显示错误
      console.log('教师记录不存在，需要完善个人信息')
    } else {
      ElMessage.error('加载个人信息失败')
    }
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
    localStorage.setItem('avatar', avatarUrl)
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
    const response = await api.put(`/teacher/${form.value.id}`, form.value)
    if (response.data.code === 200) {
      // 更新store中的头像
      if (form.value.avatar) {
        userStore.avatar = form.value.avatar
        localStorage.setItem('avatar', form.value.avatar)
      }
      ElMessage.success('保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.profile {
  padding: 20px;
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

