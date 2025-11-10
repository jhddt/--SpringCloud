<template>
  <div class="message-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息管理</span>
        </div>
      </template>
      <div style="margin-bottom: 20px;">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索消息内容"
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
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="senderName" label="发送者" width="120" />
        <el-table-column prop="receiverName" label="接收者" width="120" />
        <el-table-column prop="content" label="消息内容" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.type === 'TEXT' ? '文本' : row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
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
import api from '@/utils/api'

const tableData = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get('/message/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: searchKeyword.value
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
.message-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

