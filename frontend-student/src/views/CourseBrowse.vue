<template>
  <div class="course-browse">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程浏览</span>
          <el-input
            v-model="keyword"
            placeholder="搜索课程名称、课程代码或教师"
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
      </template>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image
              :src="getCoverImage(row)"
              style="width: 80px; height: 60px;"
              fit="cover"
              :preview-src-list="[getCoverImage(row)]"
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
            <el-button 
              v-if="row.status === 1 && !isSelected(row.id)" 
              type="primary" 
              size="small" 
              @click="handleSelect(row)"
              :disabled="isFull(row)"
            >
              {{ isFull(row) ? '已满' : '选课' }}
            </el-button>
            <el-button 
              v-else-if="isSelected(row.id)" 
              type="info" 
              size="small" 
              disabled
            >
              已选
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              @click="showCourseDetail(row)"
            >
              详情
            </el-button>
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
    
    <!-- 课程详情对话框 -->
    <el-dialog v-model="detailVisible" title="课程详情" width="600px">
      <el-descriptions :column="2" border v-if="currentCourse">
        <el-descriptions-item label="课程代码">{{ currentCourse.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="课程名称">{{ currentCourse.courseName }}</el-descriptions-item>
        <el-descriptions-item label="授课教师">{{ currentCourse.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ currentCourse.credit }}</el-descriptions-item>
        <el-descriptions-item label="总容量">{{ currentCourse.totalCapacity }}</el-descriptions-item>
        <el-descriptions-item label="已选人数">{{ currentCourse.selectedCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentCourse.status === 1 ? 'success' : 'info'">
            {{ currentCourse.status === 1 ? '开放选课' : '未开放' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">
          {{ currentCourse.description || '暂无描述' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">
          {{ formatTime(currentCourse.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">
          {{ formatTime(currentCourse.endTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()
const router = useRouter()

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedCourseIds = ref([])
const detailVisible = ref(false)
const currentCourse = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get('/course/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value,
        status: 1
      }
    })
    if (response.data.code === 200) {
      tableData.value = response.data.data.records
      total.value = response.data.data.total
      // 加载已选课程ID
      await loadSelectedCourseIds()
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadSelectedCourseIds = async () => {
  try {
    // 确保有学生ID（如果学生记录存在）
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 如果学生记录存在，加载已选课程
    if (userStore.studentId) {
      const response = await api.get('/selection/page', {
        params: { current: 1, size: 1000, studentId: userStore.studentId }
      })
      if (response.data.code === 200) {
        selectedCourseIds.value = (response.data.data.records || [])
          .filter(s => s.status === 1 || s.status === 0)
          .map(s => s.courseId)
      }
    } else {
      // 学生记录不存在，已选课程列表为空
      selectedCourseIds.value = []
    }
  } catch (error) {
    console.error('加载已选课程失败', error)
    selectedCourseIds.value = []
  }
}

const isSelected = (courseId) => {
  return selectedCourseIds.value.includes(courseId)
}

const isFull = (course) => {
  return course.selectedCount >= course.totalCapacity
}

const handleSelect = async (row) => {
  try {
    // 确保有学生ID
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    if (!userStore.studentId) {
      ElMessageBox.confirm(
        '您尚未完善个人信息，无法进行选课操作。是否前往完善个人信息？',
        '提示',
        {
          confirmButtonText: '前往完善',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        router.push('/profile')
      }).catch(() => {
        // 用户取消
      })
      return
    }
    
    const response = await api.post('/selection/select', null, {
      params: {
        studentId: userStore.studentId,
        courseId: row.id
      }
    })
    if (response.data.code === 200) {
      ElMessage.success('选课成功，等待审核')
      selectedCourseIds.value.push(row.id)
      // 重新加载数据以更新已选状态
      await loadSelectedCourseIds()
      // 刷新课程列表以更新已选人数
      loadData()
    } else {
      ElMessage.error(response.data.message || '选课失败')
    }
  } catch (error) {
    console.error('选课失败', error)
    // 处理特定的错误情况
    if (error.response?.status === 400) {
      // 400错误可能是业务逻辑错误（如已选课、课程已满等）
      const errorMsg = error.response.data?.message || '选课失败'
      ElMessage.error(errorMsg)
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error(error.message || '选课失败，请稍后重试')
    }
  }
}

const showCourseDetail = (row) => {
  currentCourse.value = row
  detailVisible.value = true
}

const getCoverImage = (course) => {
  if (!course.coverImage || course.coverImage.trim() === '') {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYfliqDovb3lpLHotKU8L3RleHQ+PC9zdmc+'
  }
  
  let imageUrl = course.coverImage
  if (imageUrl.includes('localhost:9000') || imageUrl.includes('127.0.0.1:9000')) {
    try {
      const url = new URL(imageUrl)
      let path = url.pathname
      if (path.startsWith('/')) {
        path = path.substring(1)
      }
      imageUrl = `/api/file/view?path=${encodeURIComponent(path)}`
    } catch (e) {
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYfliqDovb3lpLHotKU8L3RleHQ+PC9zdmc+'
    }
  }
  
  if (imageUrl.startsWith('/api/file/view')) {
    return imageUrl
  }
  
  return imageUrl
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
.course-browse {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

