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
              v-if="!row.editing"
              v-model="row.score"
              :min="0"
              :max="100"
              :precision="1"
              @change="handleScoreChange(row)"
              style="width: 100px;"
            />
            <span v-else>{{ row.score !== null && row.score !== undefined ? row.score : '暂无' }}</span>
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
        courseId: selectedCourseId.value,
        status: 1
      }
    })
    if (response.data.code === 200) {
      tableData.value = response.data.data.records.map(r => ({
        ...r,
        editing: false
      }))
      total.value = response.data.data.total
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleScoreChange = (row) => {
  row.editing = true
}

const handleSaveScore = async (row) => {
  try {
    const response = await api.put(`/selection/${row.id}/score`, null, {
      params: { score: row.score }
    })
    if (response.data.code === 200) {
      ElMessage.success('保存成功')
      row.editing = false
      loadData()
    }
  } catch (error) {
    ElMessage.error('保存失败')
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

