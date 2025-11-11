<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.selectedCount }}</div>
            <div class="stat-label">已选课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.pendingCount }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalCredit }}</div>
            <div class="stat-label">总学分</div>
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
            <span>我的选课</span>
          </template>
          <el-table :data="mySelections" v-loading="loading" style="width: 100%">
            <el-table-column prop="courseName" label="课程名称" />
            <el-table-column prop="credit" label="学分" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
                  {{ row.status === 1 ? '已通过' : row.status === 2 ? '已拒绝' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="selectionTime" label="选课时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.selectionTime) }}
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
            <el-table-column prop="senderName" label="发送者" width="100" />
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
  selectedCount: 0,
  pendingCount: 0,
  totalCredit: 0,
  unreadMessageCount: 0
})

const mySelections = ref([])
const recentMessages = ref([])
const loading = ref(false)

const loadStatistics = async () => {
  loading.value = true
  try {
    // 确保有学生ID（如果学生记录存在）
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 如果学生记录存在，加载选课数据
    if (userStore.studentId) {
      // 加载选课数据
      const selectionResponse = await api.get('/selection/page', {
        params: { current: 1, size: 1000, studentId: userStore.studentId }
      })
      
      if (selectionResponse.data.code === 200) {
        const selections = selectionResponse.data.data.records || []
        statistics.value.selectedCount = selections.filter(s => s.status === 1).length
        statistics.value.pendingCount = selections.filter(s => s.status === 0).length
        statistics.value.totalCredit = selections
          .filter(s => s.status === 1)
          .reduce((sum, s) => sum + (s.credit || 0), 0)
      }
    } else {
      // 学生记录不存在，统计数据保持为0
      statistics.value.selectedCount = 0
      statistics.value.pendingCount = 0
      statistics.value.totalCredit = 0
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

const loadMySelections = async () => {
  try {
    // 确保有学生ID（如果学生记录存在）
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 如果学生记录存在，加载选课数据
    if (userStore.studentId) {
      const response = await api.get('/selection/page', {
        params: { current: 1, size: 5, studentId: userStore.studentId }
      })
      if (response.data.code === 200) {
        mySelections.value = response.data.data.records || []
      }
    } else {
      // 学生记录不存在，显示空列表
      mySelections.value = []
    }
  } catch (error) {
    console.error('加载选课数据失败', error)
    mySelections.value = []
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
  loadMySelections()
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
