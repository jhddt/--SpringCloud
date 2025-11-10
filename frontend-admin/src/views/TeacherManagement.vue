<template>
  <div class="teacher-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>教师管理</span>
          <el-button type="primary" @click="handleAdd">新增教师</el-button>
        </div>
      </template>
      <div style="margin-bottom: 20px;">
        <el-input
          v-model="keyword"
          placeholder="搜索工号、姓名"
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
            <el-avatar :src="row.avatarUrl || row.avatar" :size="50" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="工号" width="120">
          <template #default="{ row }">
            {{ row.username || row.teacherNo }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100">
          <template #default="{ row }">
            {{ row.name || row.realName }}
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id || row.teacherId)">删除</el-button>
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
        <el-form-item label="工号" required>
          <el-input v-model="form.teacherNo" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.department" placeholder="请选择部门" style="width: 100%;" filterable allow-create>
            <el-option label="计算机学院" value="计算机学院" />
            <el-option label="数学学院" value="数学学院" />
            <el-option label="物理学院" value="物理学院" />
            <el-option label="电子工程学院" value="电子工程学院" />
            <el-option label="机械工程学院" value="机械工程学院" />
            <el-option label="经济管理学院" value="经济管理学院" />
            <el-option label="外国语学院" value="外国语学院" />
            <el-option label="文学院" value="文学院" />
            <el-option label="法学院" value="法学院" />
            <el-option label="化学与材料科学学院" value="化学与材料科学学院" />
            <el-option label="生命科学学院" value="生命科学学院" />
            <el-option label="艺术学院" value="艺术学院" />
            <el-option label="体育学院" value="体育学院" />
            <el-option label="马克思主义学院" value="马克思主义学院" />
            <el-option label="公共教学部" value="公共教学部" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="form.title" placeholder="请选择职称" style="width: 100%;">
            <el-option label="教授" value="教授" />
            <el-option label="副教授" value="副教授" />
            <el-option label="讲师" value="讲师" />
            <el-option label="助教" value="助教" />
            <el-option label="研究员" value="研究员" />
            <el-option label="副研究员" value="副研究员" />
            <el-option label="助理研究员" value="助理研究员" />
            <el-option label="高级工程师" value="高级工程师" />
            <el-option label="工程师" value="工程师" />
            <el-option label="助理工程师" value="助理工程师" />
          </el-select>
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
    const response = await api.get('/teacher/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value
      }
    })
    if (response.data.code === 200) {
      tableData.value = response.data.data.records
      total.value = response.data.data.total
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增教师')
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

const handleAdd = () => {
  dialogTitle.value = '新增教师'
  form.value = {
    id: null,
    teacherNo: '',
    realName: '',
    department: '',
    title: '',
    phone: '',
    email: '',
    avatar: ''
  }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑教师'
  try {
    const response = await api.get(`/teacher/${row.id || row.teacherId}`)
    if (response.data.code === 200) {
      const data = response.data.data
      // 将后端返回的字段映射到前端表单字段
      form.value = {
        id: data.teacherId || data.id, // teacherId -> id
        teacherNo: data.username || '', // username -> teacherNo (工号)
        realName: data.name || '', // name -> realName
        department: data.department || '',
        title: data.title || '',
        phone: data.phone || '',
        email: data.email || '',
        avatar: data.avatarUrl || '' // avatarUrl -> avatar
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载教师信息失败')
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
  // 前端验证
  if (!form.value.realName || !form.value.realName.trim()) {
    ElMessage.error('请输入姓名')
    return
  }
  if (!form.value.teacherNo || !form.value.teacherNo.trim()) {
    ElMessage.error('请输入工号')
    return
  }
  
  try {
    // 将前端字段名映射到后端期望的字段名
    const submitData = {
      name: form.value.realName.trim(), // realName -> name
      department: form.value.department || '',
      title: form.value.title || '',
      phone: form.value.phone || '',
      email: form.value.email || '',
      avatarUrl: form.value.avatar || '', // avatar -> avatarUrl
      username: form.value.teacherNo.trim() // teacherNo -> username (工号)
    }
    
    if (form.value.id) {
      // 更新
      const response = await api.put(`/teacher/${form.value.id}`, submitData)
      if (response.data.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      // 新增
      const response = await api.post('/teacher', submitData)
      if (response.data.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
    // 尝试获取详细的错误信息
    let errorMsg = '操作失败'
    if (error.response?.data) {
      if (error.response.data.message) {
        errorMsg = error.response.data.message
      } else if (error.response.data.code) {
        errorMsg = `错误代码: ${error.response.data.code}`
      }
    } else if (error.message) {
      errorMsg = error.message
    }
    ElMessage.error(errorMsg)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该教师吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await api.delete(`/teacher/${id}`)
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
.teacher-management {
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

