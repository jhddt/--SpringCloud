<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <el-button type="primary" @click="handleSend">发送消息</el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="收到的消息" name="received">
          <el-table :data="receivedMessages" v-loading="loading" border>
            <el-table-column prop="senderName" label="发送者" width="120" />
            <el-table-column prop="content" label="消息内容" show-overflow-tooltip />
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
            v-model:current-page="receivedPage"
            v-model:page-size="pageSize"
            :total="receivedTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadReceivedMessages"
            @current-change="loadReceivedMessages"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
        <el-tab-pane label="发送的消息" name="sent">
          <el-table :data="sentMessages" v-loading="loading" border>
            <el-table-column prop="receiverName" label="接收者" width="120" />
            <el-table-column prop="content" label="消息内容" show-overflow-tooltip />
            <el-table-column prop="createTime" label="发送时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
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
            v-model:current-page="sentPage"
            v-model:page-size="pageSize"
            :total="sentTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadSentMessages"
            @current-change="loadSentMessages"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 发送消息对话框 -->
    <el-dialog v-model="sendDialogVisible" title="发送消息" width="500px">
      <el-form :model="sendForm" label-width="80px">
        <el-form-item label="接收者" required>
          <el-select
            v-model="sendForm.receiverId"
            placeholder="请选择接收者"
            style="width: 100%;"
            filterable
          >
            <el-option-group label="学生">
              <el-option
                v-for="student in studentList"
                :key="`student-${student.id}`"
                :label="`${student.realName} (${student.studentNo})`"
                :value="student.id"
              />
            </el-option-group>
            <el-option-group label="教师">
              <el-option
                v-for="teacher in teacherList"
                :key="`teacher-${teacher.id}`"
                :label="`${teacher.realName} (${teacher.teacherNo})`"
                :value="teacher.id"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" required>
          <el-input
            v-model="sendForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入消息内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitSend">发送</el-button>
      </template>
    </el-dialog>
    
    <!-- 消息详情对话框 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="500px">
      <el-descriptions :column="1" border v-if="currentMessage">
        <el-descriptions-item label="发送者">{{ currentMessage.senderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接收者">{{ currentMessage.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="消息内容">
          <div style="white-space: pre-wrap;">{{ currentMessage.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag>{{ currentMessage.type === 'TEXT' ? '文本' : currentMessage.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ formatTime(currentMessage.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态" v-if="currentMessage.status !== undefined">
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

const activeTab = ref('received')
const receivedMessages = ref([])
const sentMessages = ref([])
const loading = ref(false)
const receivedPage = ref(1)
const sentPage = ref(1)
const pageSize = ref(20)
const receivedTotal = ref(0)
const sentTotal = ref(0)
const sendDialogVisible = ref(false)
const sendForm = ref({
  receiverId: null,
  content: ''
})
const studentList = ref([])
const teacherList = ref([])
const detailVisible = ref(false)
const currentMessage = ref(null)

const loadReceivedMessages = async () => {
  loading.value = true
  try {
    const response = await api.get('/message/page', {
      params: {
        current: receivedPage.value,
        size: pageSize.value,
        receiverId: userStore.userId
      }
    })
    
    if (response.data.code === 200) {
      receivedMessages.value = response.data.data.records || []
      receivedTotal.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载收到的消息失败', error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const loadSentMessages = async () => {
  loading.value = true
  try {
    const response = await api.get('/message/page', {
      params: {
        current: sentPage.value,
        size: pageSize.value,
        senderId: userStore.userId
      }
    })
    
    if (response.data.code === 200) {
      sentMessages.value = response.data.data.records || []
      sentTotal.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载发送的消息失败', error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const loadStudentList = async () => {
  try {
    const response = await api.get('/student/page', {
      params: { current: 1, size: 1000 }
    })
    if (response.data.code === 200) {
      studentList.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载学生列表失败', error)
  }
}

const loadTeacherList = async () => {
  try {
    const response = await api.get('/teacher/page', {
      params: { current: 1, size: 1000 }
    })
    if (response.data.code === 200) {
      teacherList.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载教师列表失败', error)
  }
}

const handleTabChange = (name) => {
  if (name === 'received') {
    loadReceivedMessages()
  } else if (name === 'sent') {
    loadSentMessages()
  }
}

const handleSend = () => {
  sendForm.value = {
    receiverId: null,
    content: ''
  }
  sendDialogVisible.value = true
}

const handleSubmitSend = async () => {
  if (!sendForm.value.receiverId || !sendForm.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const response = await api.post('/message', {
      receiverId: sendForm.value.receiverId,
      content: sendForm.value.content,
      type: 'TEXT'
    })
    if (response.data.code === 200) {
      ElMessage.success('发送成功')
      sendDialogVisible.value = false
      if (activeTab.value === 'sent') {
        loadSentMessages()
      }
    }
  } catch (error) {
    ElMessage.error('发送失败')
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
  loadReceivedMessages()
  loadStudentList()
  loadTeacherList()
})
</script>

<style scoped>
.message-center {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

