<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.courseCount }}</div>
            <div class="stat-label">我的课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.studentCount }}</div>
            <div class="stat-label">选课学生</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.pendingCount }}</div>
            <div class="stat-label">待审核选课</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.unreadMessageCount }}</div>
            <div class="stat-label">未读消息</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>我的课程</span>
          </template>
          <el-table :data="myCourses" v-loading="loading" style="width: 100%">
            <el-table-column prop="courseName" label="课程名称" />
            <el-table-column prop="selectedCount" label="已选人数" width="100" />
            <el-table-column prop="totalCapacity" label="总容量" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '开放选课' : '未开放' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近消息</span>
          </template>
          <el-table :data="recentMessages" v-loading="loading" style="width: 100%">
            <el-table-column prop="senderName" label="发送人" width="100" />
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt || row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()

const statistics = ref({
  courseCount: 0,
  studentCount: 0,
  pendingCount: 0,
  unreadMessageCount: 0
})

const myCourses = ref([])
const recentMessages = ref([])
const loading = ref(false)

const loadStatistics = async () => {
  loading.value = true
  try {
    // 加载课程数据
    const courseResponse = await api.get('/course/page', {
      params: { current: 1, size: 1000, teacherId: userStore.userId }
    })
    
    if (courseResponse.data.code === 200) {
      const courses = courseResponse.data.data.records || []
      statistics.value.courseCount = courses.length
      
      // 计算总选课学生数
      let studentIds = new Set()
      for (const course of courses) {
        const selectionResponse = await api.get('/selection/page', {
          params: { current: 1, size: 1000, courseId: course.id, status: 1 }
        })
        if (selectionResponse.data.code === 200) {
          const selections = selectionResponse.data.data.records || []
          selections.forEach(s => studentIds.add(s.studentId))
        }
      }
      statistics.value.studentCount = studentIds.size
      
      // 计算待审核选课数
      let pendingCount = 0
      for (const course of courses) {
        const selectionResponse = await api.get('/selection/page', {
          params: { current: 1, size: 1, courseId: course.id, status: 0 }
        })
        if (selectionResponse.data.code === 200) {
          pendingCount += selectionResponse.data.data.total || 0
        }
      }
      statistics.value.pendingCount = pendingCount
    }
    
    // 加载未读消息数（使用新的API，从请求头获取userId）
    try {
      const messageResponse = await api.get('/message/unread-count')
      if (messageResponse.data.code === 200) {
        statistics.value.unreadMessageCount = messageResponse.data.data || 0
      }
    } catch (error) {
      console.error('加载未读消息数失败', error)
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadMyCourses = async () => {
  try {
    const response = await api.get('/course/page', {
      params: { current: 1, size: 5, teacherId: userStore.userId }
    })
    if (response.data.code === 200) {
      myCourses.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载课程数据失败', error)
    myCourses.value = []
  }
}

const loadRecentMessages = async () => {
  try {
    const response = await api.get('/message/page', {
      params: { current: 1, size: 5 }
    })
    if (response.data.code === 200) {
      recentMessages.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载消息失败', error)
    recentMessages.value = []
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadStatistics()
  loadMyCourses()
  loadRecentMessages()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
