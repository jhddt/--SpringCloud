<template>
  <div class="grade-management">
    <el-card>
      <template #header>
        <span>成绩管理</span>
      </template>
      <el-select
        v-model="selectedCourseId"
        placeholder="请选择课程"
        style="width: 300px; margin-bottom: 20px;"
        @change="loadData"
      >
        <el-option
          v-for="course in courseList"
          :key="course.id"
          :label="course.courseName"
          :value="course.id"
        />
      </el-select>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="score" label="成绩" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.score"
              :min="0"
              :max="100"
              :precision="1"
              :disabled="row.editing"
              @change="handleScoreChange(row)"
              style="width: 100px;"
              placeholder="请输入成绩"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleSaveScore(row)"
              :disabled="row.score === null || row.score === undefined"
            >
              保存
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
const courseList = ref([])
const selectedCourseId = ref(null)

const loadCourses = async () => {
  try {
    const response = await api.get('/course/page', {
      params: { current: 1, size: 1000, teacherId: userStore.userId }
    })
    if (response.data.code === 200) {
      courseList.value = response.data.data.records || []
      if (courseList.value.length > 0 && !selectedCourseId.value) {
        selectedCourseId.value = courseList.value[0].id
        loadData()
      }
    }
  } catch (error) {
    console.error('加载课程列表失败', error)
  }
}

const loadData = async () => {
  if (!selectedCourseId.value) {
    tableData.value = []
    total.value = 0
    return
  }
  
  loading.value = true
  try {
    const response = await api.get('/selection/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        courseId: selectedCourseId.value
        // 移除 status 参数，因为新的选课流程中 status=0 表示已选
        // 只显示已选课程（status=0），已退课程（status=1）不显示
      }
    })
    if (response.data.code === 200) {
      // 只显示已选课程（status=0）
      const allRecords = response.data.data.records || []
      const selectedRecords = allRecords.filter(r => r.status === 0)
      
      // 调试：打印数据，检查学生信息
      console.log('选课记录数据:', selectedRecords)
      
      tableData.value = selectedRecords.map(r => {
        // 确保学生信息字段存在
        const record = {
          ...r,
          editing: false,
          studentName: r.studentName || '未知',
          studentNo: r.studentNo || '未知'
        }
        // 如果学生信息为空，记录警告
        if (!r.studentName || !r.studentNo) {
          console.warn('学生信息缺失:', {
            id: r.id,
            studentId: r.studentId,
            studentName: r.studentName,
            studentNo: r.studentNo
          })
        }
        return record
      })
      total.value = selectedRecords.length
      
      // 如果没有数据，提示用户
      if (selectedRecords.length === 0) {
        ElMessage.info('该课程暂无学生选课')
      }
    }
  } catch (error) {
    console.error('加载数据失败', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('加载数据失败')
    }
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleScoreChange = (row) => {
  // 成绩改变时标记为已编辑
  if (row.score !== null && row.score !== undefined) {
    row.editing = true
  }
}

const handleSaveScore = async (row) => {
  try {
    if (row.score === null || row.score === undefined || row.score === '') {
      ElMessage.warning('请输入成绩')
      return
    }
    
    // 验证成绩范围
    const score = parseFloat(row.score)
    if (isNaN(score) || score < 0 || score > 100) {
      ElMessage.warning('成绩必须在0-100之间')
      return
    }
    
    const response = await api.put(`/selection/${row.id}/score`, null, {
      params: { score: score }
    })
    
    if (response.data.code === 200) {
      ElMessage.success('保存成功')
      row.editing = false
      loadData()
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存成绩失败', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('保存失败，请稍后重试')
    }
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.grade-management {
  padding: 20px;
}
</style>

