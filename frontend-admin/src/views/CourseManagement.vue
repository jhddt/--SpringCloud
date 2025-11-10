<template>
  <div class="course-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程管理</span>
          <el-button type="primary" @click="handleAdd">新增课程</el-button>
        </div>
      </template>
      <el-input
        v-model="keyword"
        placeholder="搜索课程名称、课程代码或教师"
        style="width: 300px; margin-bottom: 20px;"
        clearable
        @clear="loadData"
      >
        <template #append>
          <el-button @click="loadData">搜索</el-button>
        </template>
      </el-input>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image
              :src="getCoverImage(row)"
              style="width: 80px; height: 60px;"
              fit="cover"
              :preview-src-list="[row.coverImage]"
            >
              <template #error>
                <div class="image-slot" style="width: 80px; height: 60px; display: flex; align-items: center; justify-content: center; background-color: #f5f5f5; color: #999; font-size: 12px;">
                  <span>暂无图片</span>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" width="120">
          <template #default="{ row }">
            {{ row.teacherName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="totalCapacity" label="总容量" width="100" />
        <el-table-column prop="selectedCount" label="已选人数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '开放选课' : '未开放' }}
            </el-tag>
          </template>
        </el-table-column>
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
      width="700px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="课程代码" required>
          <el-input v-model="form.courseCode" placeholder="请输入课程代码" />
        </el-form-item>
        <el-form-item label="课程名称" required>
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="授课教师" required>
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%;">
            <el-option
              v-for="teacher in teacherList"
              :key="teacher.teacherId || teacher.id"
              :label="teacher.name || teacher.realName"
              :value="teacher.teacherId || teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程描述"
          />
        </el-form-item>
        <el-form-item label="学分" required>
          <el-select v-model="form.credit" placeholder="请选择学分" style="width: 100%;">
            <el-option label="1" :value="1" />
            <el-option label="1.5" :value="1.5" />
            <el-option label="2" :value="2" />
            <el-option label="2.5" :value="2.5" />
            <el-option label="3" :value="3" />
            <el-option label="3.5" :value="3.5" />
            <el-option label="4" :value="4" />
            <el-option label="4.5" :value="4.5" />
            <el-option label="5" :value="5" />
            <el-option label="6" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="总容量" required>
          <el-input-number v-model="form.totalCapacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            :action="'/api/file/upload/course-cover'"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :on-error="handleCoverError"
            :before-upload="beforeCoverUpload"
          >
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-image" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">开放选课</el-radio>
            <el-radio :label="0">未开放</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%;"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%;"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
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
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get('/course/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value
      }
    })
    if (response.data.code === 200) {
      tableData.value = response.data.data.records
      total.value = response.data.data.total
      // 调试：检查教师名称
      console.log('课程数据:', tableData.value.map(c => ({
        courseName: c.courseName,
        teacherId: c.teacherId,
        teacherName: c.teacherName
      })))
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const getCoverImage = (course) => {
  // 如果封面图片URL为空或无效，返回默认占位符
  if (!course.coverImage || course.coverImage.trim() === '') {
    return getDefaultCoverImage()
  }
  
  // 如果URL是MinIO的直接访问地址，转换为通过后端代理访问
  // 例如：http://localhost:9000/education-files/course-covers/xxx.jpg
  // 转换为：/api/file/view?path=education-files/course-covers/xxx.jpg
  let imageUrl = course.coverImage
  
  // 检查是否是MinIO URL（包含 localhost:9000 或 127.0.0.1:9000）
  if (imageUrl.includes('localhost:9000') || imageUrl.includes('127.0.0.1:9000')) {
    // 提取路径部分：从URL中提取 bucket-name/object-name
    try {
      const url = new URL(imageUrl)
      // 移除开头的 /，得到路径：education-files/course-covers/xxx.jpg
      let path = url.pathname
      if (path.startsWith('/')) {
        path = path.substring(1)
      }
      // 转换为代理URL
      imageUrl = `/api/file/view?path=${encodeURIComponent(path)}`
    } catch (e) {
      // URL解析失败，使用默认图片
      console.error('解析图片URL失败:', e)
      return getDefaultCoverImage()
    }
  }
  
  // 如果已经是代理URL格式，直接返回
  if (imageUrl.startsWith('/api/file/view')) {
    return imageUrl
  }
  
  // 如果是其他格式的URL，直接返回（可能是外部URL）
  return imageUrl
}

const getDefaultCoverImage = () => {
  // 使用 data URI 作为默认占位符图片（一个简单的灰色占位符）
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYfliqDovb3lpLHotKU8L3RleHQ+PC9zdmc+'
}


const dialogVisible = ref(false)
const dialogTitle = ref('新增课程')
const form = ref({
  id: null,
  courseCode: '',
  courseName: '',
  teacherId: null,
  description: '',
  totalCapacity: 0,
  credit: 0,
  coverImage: '',
  status: 1,
  startTime: '',
  endTime: ''
})

const teacherList = ref([])

const loadTeachers = async () => {
  try {
    const response = await api.get('/teacher/page', {
      params: { current: 1, size: 100 }
    })
    if (response.data.code === 200) {
      teacherList.value = response.data.data.records
    }
  } catch (error) {
    console.error('加载教师列表失败', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增课程'
  form.value = {
    id: null,
    courseCode: '',
    courseName: '',
    teacherId: null,
    description: '',
    totalCapacity: 0,
    credit: 0,
    coverImage: '',
    status: 1,
    startTime: '',
    endTime: ''
  }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑课程'
  try {
    const response = await api.get(`/course/${row.id}`)
    if (response.data.code === 200) {
      const data = response.data.data
      form.value = {
        id: data.id,
        courseCode: data.courseCode,
        courseName: data.courseName,
        teacherId: data.teacherId,
        description: data.description || '',
        totalCapacity: data.totalCapacity || 0,
        credit: data.credit || 0,
        coverImage: data.coverImage || '',
        status: data.status || 1,
        startTime: data.startTime ? data.startTime.replace('T', ' ') : '',
        endTime: data.endTime ? data.endTime.replace('T', ' ') : ''
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载课程信息失败')
  }
}

const handleSubmit = async () => {
  try {
    const submitData = {
      ...form.value,
      startTime: form.value.startTime ? form.value.startTime.replace(' ', 'T') : null,
      endTime: form.value.endTime ? form.value.endTime.replace(' ', 'T') : null
    }
    if (form.value.id) {
      // 更新
      const response = await api.put(`/course/${form.value.id}`, submitData)
      if (response.data.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      // 新增
      const response = await api.post('/course', submitData)
      if (response.data.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } catch (error) {
    ElMessage.error(form.value.id ? '更新失败' : '新增失败')
  }
}

const handleCoverSuccess = (response) => {
  if (response.code === 200) {
    form.value.coverImage = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '封面上传失败')
  }
}

const handleCoverError = (error) => {
  console.error('封面上传错误:', error)
  const errorMessage = error?.response?.data?.message || error?.message || '封面上传失败，请稍后重试'
  ElMessage.error(errorMessage)
}

const beforeCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const uploadHeaders = computed(() => {
  const userStore = useUserStore()
  return {
    Authorization: `Bearer ${userStore.token}`
  }
})

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该课程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await api.delete(`/course/${row.id}`)
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
  loadTeachers()
})
</script>

<style scoped>
.course-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 200px;
  height: 150px;
}

.cover-uploader:hover {
  border-color: #409EFF;
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 200px;
  height: 150px;
  line-height: 150px;
  text-align: center;
}

.cover-image {
  width: 200px;
  height: 150px;
  display: block;
  object-fit: cover;
}
</style>

