<template>
  <div class="grade-query">
    <el-card>
      <template #header>
        <span>成绩查询</span>
      </template>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="score" label="成绩" width="100">
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" :style="{ color: getScoreColor(row.score), fontWeight: 'bold' }">
              {{ row.score }}
            </span>
            <span v-else style="color: #999;">暂无成绩</span>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.score !== null && row.score !== undefined" :type="getGradeType(row.score)">
              {{ getGrade(row.score) }}
            </el-tag>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="selectionTime" label="选课时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.selectionTime) }}
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 20px; padding: 20px; background-color: #f5f7fa; border-radius: 4px;">
        <el-row :gutter="20">
          <el-col :span="6">
            <div style="text-align: center;">
              <div style="font-size: 24px; font-weight: bold; color: #409EFF;">{{ statistics.totalCredit }}</div>
              <div style="color: #909399; margin-top: 5px;">总学分</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div style="text-align: center;">
              <div style="font-size: 24px; font-weight: bold; color: #67C23A;">{{ statistics.avgScore.toFixed(2) }}</div>
              <div style="color: #909399; margin-top: 5px;">平均分</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div style="text-align: center;">
              <div style="font-size: 24px; font-weight: bold; color: #E6A23C;">{{ statistics.gpa.toFixed(2) }}</div>
              <div style="color: #909399; margin-top: 5px;">GPA</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div style="text-align: center;">
              <div style="font-size: 24px; font-weight: bold; color: #F56C6C;">{{ statistics.passedCount }} / {{ statistics.totalCount }}</div>
              <div style="color: #909399; margin-top: 5px;">通过/总数</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const userStore = useUserStore()

const tableData = ref([])
const loading = ref(false)
const statistics = ref({
  totalCredit: 0,
  avgScore: 0,
  gpa: 0,
  passedCount: 0,
  totalCount: 0
})

const loadData = async () => {
  loading.value = true
  try {
    // 确保有学生ID（如果学生记录存在）
    if (!userStore.studentId) {
      await userStore.loadStudentId()
    }
    
    // 如果学生记录存在，加载成绩数据
    if (userStore.studentId) {
      const response = await api.get('/selection/page', {
        params: {
          current: 1,
          size: 1000,
          studentId: userStore.studentId
          // 移除 status 参数，因为新的选课流程中 status=0 表示已选
          // 只显示已选课程（status=0），已退课程（status=1）不显示
        }
      })
      if (response.data.code === 200) {
        // 只显示已选课程（status=0）
        const allRecords = response.data.data.records || []
        tableData.value = allRecords.filter(r => r.status === 0)
        calculateStatistics()
      } else {
        ElMessage.error(response.data.message || '加载数据失败')
        tableData.value = []
        calculateStatistics()
      }
    } else {
      // 学生记录不存在，显示空列表
      tableData.value = []
      calculateStatistics()
    }
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
    tableData.value = []
    calculateStatistics()
  } finally {
    loading.value = false
  }
}

const calculateStatistics = () => {
  // 只统计已选课程（status=0），tableData 已经过滤了
  const records = tableData.value
  statistics.value.totalCount = records.length
  
  // 计算总学分
  statistics.value.totalCredit = records.reduce((sum, r) => sum + (r.credit || 0), 0)
  
  // 计算平均分和GPA
  const scoredRecords = records.filter(r => r.score !== null && r.score !== undefined)
  statistics.value.passedCount = scoredRecords.filter(r => r.score >= 60).length
  
  if (scoredRecords.length > 0) {
    const totalScore = scoredRecords.reduce((sum, r) => sum + (r.score || 0), 0)
    statistics.value.avgScore = totalScore / scoredRecords.length
    
    // 计算GPA (4.0制)
    let totalGpaPoints = 0
    let totalCredits = 0
    scoredRecords.forEach(r => {
      const credit = r.credit || 0
      const gpaPoint = scoreToGpa(r.score)
      totalGpaPoints += gpaPoint * credit
      totalCredits += credit
    })
    statistics.value.gpa = totalCredits > 0 ? totalGpaPoints / totalCredits : 0
  } else {
    statistics.value.avgScore = 0
    statistics.value.gpa = 0
  }
}

const scoreToGpa = (score) => {
  if (score >= 90) return 4.0
  if (score >= 85) return 3.7
  if (score >= 82) return 3.3
  if (score >= 78) return 3.0
  if (score >= 75) return 2.7
  if (score >= 72) return 2.3
  if (score >= 68) return 2.0
  if (score >= 64) return 1.5
  if (score >= 60) return 1.0
  return 0
}

const getScoreColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 80) return '#409EFF'
  if (score >= 70) return '#E6A23C'
  if (score >= 60) return '#909399'
  return '#F56C6C'
}

const getGrade = (score) => {
  if (score >= 90) return 'A'
  if (score >= 80) return 'B'
  if (score >= 70) return 'C'
  if (score >= 60) return 'D'
  return 'F'
}

const getGradeType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  if (score >= 60) return 'info'
  return 'danger'
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
.grade-query {
  padding: 20px;
}
</style>

