<template>
  <div class="profile">
    <el-card>
      <template #header>
        <span>个人信息</span>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 600px;">
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
        <el-form-item label="学号" prop="studentNo" required>
          <el-input v-model="form.studentNo" placeholder="请输入学号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName" required>
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="班级" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
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

const formRef = ref(null)

const form = ref({
  id: null,
  userId: null,
  studentNo: '',
  realName: '',
  major: '',
  grade: '',
  className: '',
  phone: '',
  email: '',
  avatar: ''
})

const rules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ]
}

const loadData = async () => {
  try {
    // 先确保尝试加载studentId
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 通过userId获取学生信息
    const response = await api.get(`/student/user/${userStore.userId}`)
    if (response.data.code === 200 && response.data.data) {
      const data = response.data.data
      // 将后端返回的字段映射到前端表单字段
      form.value = {
        id: data.studentId, // studentId -> id
        userId: userStore.userId,
        studentNo: data.username, // username -> studentNo (学号)
        realName: data.name, // name -> realName
        major: data.major || '',
        grade: data.grade || '',
        className: data.className || '',
        phone: data.phone || '',
        email: data.email || '',
        avatar: data.avatarUrl || '' // avatarUrl -> avatar
      }
      // 更新store中的studentId
      if (data.studentId) {
        userStore.studentId = data.studentId
        localStorage.setItem('studentId', data.studentId)
      }
    } else {
      // 学生信息不存在，初始化表单（不显示警告，因为这是正常情况）
      form.value = {
        id: null,
        userId: userStore.userId,
        studentNo: '',
        realName: userStore.username || '',
        major: '',
        grade: '',
        className: '',
        phone: '',
        email: '',
        avatar: userStore.avatar || ''
      }
    }
  } catch (error) {
    console.error('加载个人信息失败', error)
    // 如果是404错误，说明学生信息不存在（这是正常情况，不需要警告）
    if (error.response?.status === 404 || error.response?.data?.code === 404) {
      // 学生信息不存在，初始化表单
      form.value = {
        id: null,
        userId: userStore.userId,
        studentNo: '',
        realName: userStore.username || '',
        major: '',
        grade: '',
        className: '',
        phone: '',
        email: '',
        avatar: userStore.avatar || ''
      }
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
  // 验证表单
  if (!formRef.value) {
    return
  }
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    
    try {
      // 如果没有学生ID，说明是新学生，需要创建
      if (!form.value.id) {
        // 创建新学生记录，使用当前用户的userId
        // 将前端字段名映射到后端期望的字段名
        const createData = {
          userId: userStore.userId, // 传入userId，用于关联现有的user_credentials记录
          username: form.value.studentNo, // studentNo -> username (学号)
          name: form.value.realName, // realName -> name
          major: form.value.major || '',
          grade: form.value.grade || '',
          className: form.value.className || '',
          phone: form.value.phone || '',
          email: form.value.email || '',
          avatarUrl: form.value.avatar || '' // avatar -> avatarUrl
        }
        
        const createResponse = await api.post('/student', createData)
        if (createResponse.data.code === 200) {
          // 从响应中获取创建的学生ID
          const createdStudent = createResponse.data.data
          if (createdStudent && createdStudent.studentId) {
            // 立即设置 studentId（后端返回的是studentId，不是id）
            userStore.studentId = createdStudent.studentId
            localStorage.setItem('studentId', createdStudent.studentId)
            // 更新表单数据，将后端返回的字段映射到前端字段
            form.value.id = createdStudent.studentId
            form.value.studentNo = createdStudent.username || form.value.studentNo
            form.value.realName = createdStudent.name || form.value.realName
            form.value.avatar = createdStudent.avatarUrl || form.value.avatar
          } else {
            // 如果响应中没有ID，重新加载
            await loadData()
            await userStore.loadStudentId()
          }
          
          ElMessage.success('创建成功，现在可以进行选课了')
          
          // 更新用户信息
          if (form.value.realName) {
            userStore.username = form.value.realName
            localStorage.setItem('username', form.value.realName)
          }
          if (form.value.avatar) {
            userStore.avatar = form.value.avatar
            localStorage.setItem('avatar', form.value.avatar)
          }
        }
      } else {
        // 确保使用的是学生ID，而不是用户ID
        if (form.value.id === userStore.userId) {
          ElMessage.error('学生信息错误，请刷新页面后重试')
          await loadData()
          return
        }
        
        // 更新现有学生记录
        // 将前端字段名映射到后端期望的字段名
        const updateData = {
          name: form.value.realName, // realName -> name
          major: form.value.major || '',
          grade: form.value.grade || '',
          className: form.value.className || '',
          phone: form.value.phone || '',
          email: form.value.email || '',
          avatarUrl: form.value.avatar || '' // avatar -> avatarUrl
        }
        const response = await api.put(`/student/${form.value.id}`, updateData)
        if (response.data.code === 200) {
          ElMessage.success('保存成功')
          // 清除缓存，重新加载数据
          await loadData()
          // 更新用户信息
          if (form.value.realName) {
            userStore.username = form.value.realName
            localStorage.setItem('username', form.value.realName)
          }
          if (form.value.avatar) {
            userStore.avatar = form.value.avatar
            localStorage.setItem('avatar', form.value.avatar)
          }
        }
      }
    } catch (error) {
      console.error('保存失败', error)
      const errorMsg = error.response?.data?.message || error.message || '保存失败'
      ElMessage.error(errorMsg)
    }
  })
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

