<template>
  <div class="dashboard">
    <el-row :gutter="24">
      <el-col :span="6" v-for="(stat, index) in statCards" :key="index">
        <el-card class="stat-card card-hover" :class="`stat-card-${index}`">
          <div class="stat-item">
            <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
              <el-icon :size="32" class="stat-icon"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
            <div class="stat-trend">
              <el-icon :size="16" class="trend-icon"><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card class="table-card card-hover">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><List /></el-icon>
              <span>最近选课</span>
            </div>
          </template>
          <el-table :data="recentSelections" v-loading="loading" style="width: 100%" class="custom-table">
            <el-table-column prop="studentName" label="学生" width="100" />
            <el-table-column prop="courseName" label="课程" />
            <el-table-column prop="selectionTime" label="选课时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag 
                  :type="row.status === 0 ? 'success' : 'info'"
                  effect="dark"
                  round
                >
                  {{ row.status === 0 ? '已选' : '已退' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="table-card card-hover">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Trophy /></el-icon>
              <span>热门课程</span>
            </div>
          </template>
          <el-table :data="popularCourses" v-loading="loading" style="width: 100%" class="custom-table">
            <el-table-column prop="courseName" label="课程名称" />
            <el-table-column prop="teacherName" label="授课教师" width="120" />
            <el-table-column prop="selectedCount" label="已选人数" width="100" />
            <el-table-column prop="totalCapacity" label="总容量" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { User, Avatar, Document, List, Trophy, TrendCharts } from '@element-plus/icons-vue'

const statistics = ref({
  studentCount: 0,
  teacherCount: 0,
  courseCount: 0,
  selectionCount: 0
})

const recentSelections = ref([])
const popularCourses = ref([])
const loading = ref(false)

const statCards = computed(() => [
  {
    icon: User,
    label: '学生总数',
    value: statistics.value.studentCount,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    icon: Avatar,
    label: '教师总数',
    value: statistics.value.teacherCount,
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    icon: Document,
    label: '课程总数',
    value: statistics.value.courseCount,
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    icon: List,
    label: '选课总数',
    value: statistics.value.selectionCount,
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

const loadStatistics = async () => {
  loading.value = true
  try {
    // 并行加载所有统计数据
    const [studentRes, teacherRes, courseRes, selectionRes] = await Promise.allSettled([
      api.get('/student/page', { params: { current: 1, size: 1 } }),
      api.get('/teacher/page', { params: { current: 1, size: 1 } }),
      api.get('/course/page', { params: { current: 1, size: 1 } }),
      api.get('/selection/page', { params: { current: 1, size: 1 } })
    ])
    
    // 处理学生总数
    if (studentRes.status === 'fulfilled' && studentRes.value.data.code === 200) {
      const studentData = studentRes.value.data.data
      console.log('学生API响应:', studentData)
      statistics.value.studentCount = studentData?.total || 0
      console.log('学生总数:', statistics.value.studentCount, '记录数:', studentData?.records?.length || 0)
    } else {
      console.error('加载学生总数失败', studentRes.reason)
    }
    
    // 处理教师总数
    if (teacherRes.status === 'fulfilled' && teacherRes.value.data.code === 200) {
      const teacherData = teacherRes.value.data.data
      console.log('教师API响应:', teacherData)
      statistics.value.teacherCount = teacherData?.total || 0
      console.log('教师总数:', statistics.value.teacherCount, '记录数:', teacherData?.records?.length || 0)
    } else {
      console.error('加载教师总数失败', teacherRes.reason)
    }
    
    // 处理课程总数
    if (courseRes.status === 'fulfilled' && courseRes.value.data.code === 200) {
      const courseData = courseRes.value.data.data
      console.log('课程API响应:', courseData)
      statistics.value.courseCount = courseData?.total || 0
      console.log('课程总数:', statistics.value.courseCount, '记录数:', courseData?.records?.length || 0)
    } else {
      console.error('加载课程总数失败', courseRes.reason)
    }
    
    // 处理选课总数
    if (selectionRes.status === 'fulfilled' && selectionRes.value.data.code === 200) {
      const selectionData = selectionRes.value.data.data
      console.log('选课API响应:', selectionData)
      statistics.value.selectionCount = selectionData?.total || 0
      console.log('选课总数:', statistics.value.selectionCount, '记录数:', selectionData?.records?.length || 0)
    } else {
      console.error('加载选课总数失败', selectionRes.reason)
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadRecentSelections = async () => {
  try {
    const response = await api.get('/selection/page', {
      params: { current: 1, size: 5 }
    })
    if (response.data.code === 200 && response.data.data) {
      recentSelections.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载最近选课失败', error)
    recentSelections.value = []
  }
}

const loadPopularCourses = async () => {
  try {
    const response = await api.get('/course/page', {
      params: { current: 1, size: 10, status: 1 }
    })
    if (response.data.code === 200 && response.data.data) {
      // 按已选人数排序
      const courses = response.data.data.records || []
      popularCourses.value = courses
        .sort((a, b) => (b.selectedCount || 0) - (a.selectedCount || 0))
        .slice(0, 5)
    }
  } catch (error) {
    console.error('加载热门课程失败', error)
    popularCourses.value = []
  }
}

onMounted(() => {
  loadStatistics()
  loadRecentSelections()
  loadPopularCourses()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  background: white;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gradient-primary);
}

.stat-card-1::before {
  background: var(--gradient-secondary);
}

.stat-card-2::before {
  background: var(--gradient-success);
}

.stat-card-3::before {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  position: relative;
}

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.stat-trend {
  position: absolute;
  top: 12px;
  right: 12px;
  color: var(--text-light);
}

.trend-icon {
  opacity: 0.5;
}

.table-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-icon {
  color: var(--primary-color);
  font-size: 20px;
}

.custom-table :deep(.el-table__header) {
  background: var(--bg-secondary);
}

.custom-table :deep(.el-table__header th) {
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.custom-table :deep(.el-table__body tr:hover) {
  background: #f8fafc;
}

.custom-table :deep(.el-table__row) {
  transition: all var(--transition-fast);
}
</style>

