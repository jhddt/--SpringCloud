<template>
  <div class="my-selection">
    <el-card>
      <template #header>
        <span>我的选课</span>
      </template>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image
              :src="getCoverImage(row)"
              style="width: 80px; height: 60px;"
              fit="cover"
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
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="selectionTime" label="选课时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.selectionTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="100">
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined">{{ row.score }}</span>
            <span v-else style="color: #999;">暂无</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              type="danger" 
              size="small" 
              @click="handleCancel(row)"
            >
              退课
            </el-button>
            <span v-else style="color: #999;">已退课</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    // 确保有学生ID（如果学生记录存在）
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 如果学生记录存在，加载选课数据
    if (userStore.studentId) {
      const response = await api.get('/selection/page', {
        params: {
          current: currentPage.value,
          size: pageSize.value,
          studentId: userStore.studentId
        }
      })
      if (response.data.code === 200) {
        // 只显示已选课程（status=0），已退课程（status=1）可以根据需要显示或隐藏
        // 这里我们显示所有记录，包括已退课的
        tableData.value = response.data.data.records || []
        total.value = response.data.data.total || 0
      } else {
        ElMessage.error(response.data.message || '加载数据失败')
        tableData.value = []
        total.value = 0
      }
    } else {
      // 学生记录不存在，显示空列表
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    0: '已选',  // 0-已选（正常状态）
    1: '已退'   // 1-已退
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'success',  // 已选 - 成功状态
    1: 'info'      // 已退 - 信息状态
  }
  return typeMap[status] || 'info'
}

const handleCancel = async (row) => {
  try {
    // 确保有学生ID
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    if (!userStore.studentId) {
      ElMessage.error('学生信息不存在')
      return
    }
    
    await ElMessageBox.confirm('确定要退课吗？退课后将无法恢复，课程容量会释放。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 使用正确的接口：PUT /selection/{id}/cancel
    const response = await api.put(`/selection/${row.id}/cancel`, null, {
      params: { studentId: userStore.studentId }
    })
    if (response.data.code === 200) {
      ElMessage.success(response.data.message || '退课成功')
      loadData()
    } else {
      ElMessage.error(response.data.message || '退课失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退课失败', error)
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('退课失败，请稍后重试')
      }
    }
  }
}

const getCoverImage = (row) => {
  // 如果封面图片URL为空或无效，返回默认占位符
  if (!row.coverImage || row.coverImage.trim() === '') {
    return getDefaultCoverImage()
  }
  
  // 如果URL是MinIO的直接访问地址，转换为通过后端代理访问
  // 例如：http://localhost:9000/education-files/course-covers/xxx.jpg
  // 转换为：/api/file/view?path=education-files/course-covers/xxx.jpg
  let imageUrl = row.coverImage
  
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

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-selection {
  padding: 20px;
}
</style>

