<template>
  <div class="student-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学生管理</span>
          <el-button type="primary" @click="handleAdd">新增学生</el-button>
        </div>
      </template>
      <div style="margin-bottom: 20px;">
        <el-input
          v-model="keyword"
          placeholder="搜索学号、姓名"
          style="width: 300px;"
          clearable
          @clear="loadData"
          @keyup.enter="loadData"
        >
          <template #append>
            <el-button @click="loadData">搜索</el-button>
          </template>
        </el-input>
      </div>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="50" />
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="学号" required>
          <el-input v-model="form.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="form.major" placeholder="请选择专业" style="width: 100%;" filterable allow-create>
            <el-option label="计算机科学与技术" value="计算机科学与技术" />
            <el-option label="软件工程" value="软件工程" />
            <el-option label="网络工程" value="网络工程" />
            <el-option label="信息安全" value="信息安全" />
            <el-option label="数据科学与大数据技术" value="数据科学与大数据技术" />
            <el-option label="人工智能" value="人工智能" />
            <el-option label="数学与应用数学" value="数学与应用数学" />
            <el-option label="信息与计算科学" value="信息与计算科学" />
            <el-option label="统计学" value="统计学" />
            <el-option label="应用物理学" value="应用物理学" />
            <el-option label="电子信息工程" value="电子信息工程" />
            <el-option label="通信工程" value="通信工程" />
            <el-option label="自动化" value="自动化" />
            <el-option label="电气工程及其自动化" value="电气工程及其自动化" />
            <el-option label="机械工程" value="机械工程" />
            <el-option label="工商管理" value="工商管理" />
            <el-option label="市场营销" value="市场营销" />
            <el-option label="会计学" value="会计学" />
            <el-option label="金融学" value="金融学" />
            <el-option label="国际经济与贸易" value="国际经济与贸易" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%;">
            <el-option label="2021" value="2021" />
            <el-option label="2022" value="2022" />
            <el-option label="2023" value="2023" />
            <el-option label="2024" value="2024" />
            <el-option label="2025" value="2025" />
            <el-option label="2026" value="2026" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="form.className" placeholder="请输入班级（如：计科2024-1班）" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const keyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get('/student/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value
      }
    })
    if (response.data.code === 200) {
      // 将后端返回的字段映射到前端期望的字段名
      tableData.value = response.data.data.records.map(item => ({
        id: item.studentId, // studentId -> id
        studentNo: item.username, // username -> studentNo (学号)
        realName: item.name, // name -> realName
        major: item.major,
        grade: item.grade,
        className: item.className,
        phone: item.phone,
        email: item.email,
        avatar: item.avatarUrl // avatarUrl -> avatar
      }))
      total.value = response.data.data.total
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const form = ref({
  id: null,
  studentNo: '',
  realName: '',
  major: '',
  grade: '',
  className: '',
  phone: '',
  email: '',
  avatar: ''
})

const handleAdd = () => {
  dialogTitle.value = '新增学生'
  form.value = {
    id: null,
    studentNo: '',
    realName: '',
    major: '',
    grade: '',
    className: '',
    phone: '',
    email: '',
    avatar: ''
  }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑学生'
  try {
    const response = await api.get(`/student/${row.id}`)
    if (response.data.code === 200) {
      const data = response.data.data
      // 将后端返回的字段映射到前端表单字段
      form.value = {
        id: data.studentId, // studentId -> id
        studentNo: data.username, // username -> studentNo (学号)
        realName: data.name, // name -> realName
        major: data.major,
        grade: data.grade,
        className: data.className,
        phone: data.phone,
        email: data.email,
        avatar: data.avatarUrl // avatarUrl -> avatar
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载学生信息失败')
  }
}

const userStore = useUserStore()

const uploadAction = computed(() => '/api/file/upload/avatar')
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    form.value.avatar = response.data.url
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
    // 构建请求数据，将前端字段名映射到后端期望的字段名
    const requestData = {
      name: form.value.realName, // realName -> name
      username: form.value.studentNo, // studentNo -> username (学号)
      major: form.value.major,
      grade: form.value.grade,
      className: form.value.className,
      phone: form.value.phone,
      email: form.value.email,
      avatarUrl: form.value.avatar // avatar -> avatarUrl
    }
    
    if (form.value.id) {
      // 更新
      const response = await api.put(`/student/${form.value.id}`, requestData)
      if (response.data.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      // 新增
      const response = await api.post('/student', requestData)
      if (response.data.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } catch (error) {
    console.error('提交错误:', error)
    const errorMessage = error?.response?.data?.message || (form.value.id ? '更新失败' : '新增失败')
    ElMessage.error(errorMessage)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该学生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await api.delete(`/student/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.student-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

