<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <span>消息中心</span>
      </template>
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="senderName" label="发送者" width="120" />
        <el-table-column prop="content" label="消息内容" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.type === 'TEXT' ? '文本' : row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              type="primary" 
              size="small" 
              @click="handleMarkAsRead(row)"
            >
              标记已读
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              @click="showMessageDetail(row)"
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
    
    <!-- 消息详情对话框 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="500px">
      <el-descriptions :column="1" border v-if="currentMessage">
        <el-descriptions-item label="发送者">{{ currentMessage.senderName }}</el-descriptions-item>
        <el-descriptions-item label="消息内容">
          <div style="white-space: pre-wrap;">{{ currentMessage.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag>{{ currentMessage.type === 'TEXT' ? '文本' : currentMessage.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ formatTime(currentMessage.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentMessage.status === 1 ? 'success' : 'warning'">
            {{ currentMessage.status === 1 ? '已读' : '未读' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
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
const pageSize = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const currentMessage = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get('/message/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        receiverId: userStore.userId
      }
    })
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载消息失败', error)
    ElMessage.error('加载消息失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleMarkAsRead = async (row) => {
  try {
    const response = await api.put(`/message/${row.id}/read?userId=${userStore.userId}`)
    if (response.data.code === 200) {
      ElMessage.success('标记成功')
      row.status = 1
    }
  } catch (error) {
    console.error('标记已读失败', error)
    ElMessage.error('标记失败')
  }
}

const showMessageDetail = (row) => {
  currentMessage.value = row
  detailVisible.value = true
  // 如果未读，自动标记为已读
  if (row.status === 0) {
    handleMarkAsRead(row)
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
.message-center {
  padding: 20px;
}
</style>

