<template>
  <div class="selection-management">
    <el-card>
      <template #header>
        <span>选课管理</span>
      </template>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="status" label="状态" width="120">
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              type="success" 
              size="small" 
              @click="handleApprove(row, 1)"
            >
              通过
            </el-button>
            <el-button 
              v-if="row.status === 0" 
              type="danger" 
              size="small" 
              @click="handleApprove(row, 2)"
            >
              拒绝
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
    // 先获取教师的课程ID列表
    const courseResponse = await api.get('/course/page', {
      params: { current: 1, size: 1000, teacherId: userStore.userId }
    })
    
    if (courseResponse.data.code === 200) {
      const courses = courseResponse.data.data.records || []
      const courseIds = courses.map(c => c.id)
      
      if (courseIds.length === 0) {
        tableData.value = []
        total.value = 0
        loading.value = false
        return
      }
      
      // 获取所有课程的选课记录
      const allSelections = []
      for (const courseId of courseIds) {
        const selectionResponse = await api.get('/selection/page', {
          params: {
            current: 1,
            size: 1000,
            courseId: courseId
          }
        })
        if (selectionResponse.data.code === 200) {
          const selections = selectionResponse.data.data.records || []
          allSelections.push(...selections)
        }
      }
      
      // 分页处理
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = allSelections.slice(start, end)
      total.value = allSelections.length
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

const handleApprove = async (row, status) => {
  try {
    const response = await api.put(`/selection/${row.id}/approve`, null, {
      params: { status }
    })
    if (response.data.code === 200) {
      ElMessage.success('操作成功')
      loadData()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
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
.selection-management {
  padding: 20px;
}
</style>

