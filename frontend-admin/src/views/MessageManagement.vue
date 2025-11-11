<template>
  <div class="message-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息管理</span>
          <div>
            <el-select
              v-model="filterMessageType"
              placeholder="消息类型"
              clearable
              style="width: 150px; margin-right: 10px;"
              @change="loadData"
            >
              <el-option label="全部" value="" />
              <el-option label="即时消息" value="INSTANT_MESSAGE" />
              <el-option label="系统通知" value="SYSTEM_NOTICE" />
              <el-option label="互动提醒" value="INTERACTION_REMINDER" />
              <el-option label="平台公告" value="PLATFORM_ANNOUNCEMENT" />
            </el-select>
            <el-select
              v-model="filterScopeType"
              placeholder="范围类型"
              clearable
              style="width: 150px; margin-right: 10px;"
              @change="loadData"
            >
              <el-option label="全部" value="" />
              <el-option label="私聊" value="PRIVATE" />
              <el-option label="课程" value="COURSE" />
              <el-option label="群组" value="GROUP" />
              <el-option label="全局" value="GLOBAL" />
            </el-select>
            <el-button type="primary" @click="handleSend">发送通知</el-button>
          </div>
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
        <el-table-column prop="content" label="消息内容" show-overflow-tooltip min-width="200" />
        <el-table-column prop="messageType" label="消息类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getMessageTypeTagType(row.messageType)">
              {{ getMessageTypeText(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scopeType" label="范围" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{ getScopeTypeText(row.scopeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发送时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
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
    
    <!-- 发送通知对话框 -->
    <el-dialog v-model="sendDialogVisible" title="发送通知" width="600px">
      <el-form :model="sendForm" label-width="100px" :rules="sendRules" ref="sendFormRef">
        <el-form-item label="消息类型" prop="messageType">
          <el-tag type="danger">平台公告</el-tag>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">（仅管理员可发送）</span>
        </el-form-item>
        <el-form-item label="发送范围" prop="scopeType" required>
          <el-radio-group v-model="sendForm.scopeType" @change="handleScopeChange">
            <el-radio label="GLOBAL">全体用户</el-radio>
            <el-radio label="TEACHER_ONLY">仅教师</el-radio>
            <el-radio label="STUDENT_ONLY">仅学生</el-radio>
          </el-radio-group>
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            <span v-if="sendForm.scopeType === 'GLOBAL'">所有用户（管理员、教师、学生）都可以看到</span>
            <span v-else-if="sendForm.scopeType === 'TEACHER_ONLY'">只有教师可以看到此通知</span>
            <span v-else-if="sendForm.scopeType === 'STUDENT_ONLY'">只有学生可以看到此通知</span>
          </div>
        </el-form-item>
        <el-form-item label="消息内容" prop="content" required>
          <el-input
            v-model="sendForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入通知内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitSend" :loading="sending">发送</el-button>
      </template>
    </el-dialog>
    
    <!-- 消息详情对话框 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="500px">
      <el-descriptions :column="1" border v-if="currentMessage">
        <el-descriptions-item label="发送者">{{ currentMessage.senderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接收者">{{ currentMessage.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="消息类型">
          <el-tag :type="getMessageTypeTagType(currentMessage.messageType)">
            {{ getMessageTypeText(currentMessage.messageType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="范围类型">
          <el-tag size="small" type="info">
            {{ getScopeTypeText(currentMessage.scopeType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消息内容">
          <div style="white-space: pre-wrap;">{{ currentMessage.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ formatTime(currentMessage.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentMessage.status === 1 ? 'success' : 'info'">
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
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterMessageType = ref('')
const filterScopeType = ref('')
const sendDialogVisible = ref(false)
const sending = ref(false)
const sendForm = ref({
  messageType: 'PLATFORM_ANNOUNCEMENT',
  scopeType: 'GLOBAL',
  contentType: 'TEXT',
  content: ''
})
const sendRules = {
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}
const sendFormRef = ref(null)
const detailVisible = ref(false)
const currentMessage = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (filterMessageType.value) {
      params.messageType = filterMessageType.value
    }
    if (filterScopeType.value) {
      params.scopeType = filterScopeType.value
    }
    
    const response = await api.get('/message/page', { params })
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载消息失败', error)
    ElMessage.error(error.response?.data?.message || '加载消息失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSend = () => {
  sendForm.value = {
    messageType: 'PLATFORM_ANNOUNCEMENT',
    scopeType: 'GLOBAL',
    contentType: 'TEXT',
    content: ''
  }
  sendDialogVisible.value = true
}

const handleScopeChange = () => {
  // 范围改变时的处理（如果需要）
}

const handleSubmitSend = async () => {
  if (!sendFormRef.value) return
  
  await sendFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    
    if (!sendForm.value.content) {
      ElMessage.warning('请输入通知内容')
      return
    }
    
    sending.value = true
    try {
      // 根据选择的范围设置 roleMask
      let roleMask = ''
      let actualScopeType = 'GLOBAL'
      
      if (sendForm.value.scopeType === 'TEACHER_ONLY') {
        roleMask = 'TEACHER'
        actualScopeType = 'GLOBAL'
      } else if (sendForm.value.scopeType === 'STUDENT_ONLY') {
        roleMask = 'STUDENT'
        actualScopeType = 'GLOBAL'
      } else {
        // GLOBAL - 所有人可见
        roleMask = 'ADMIN,TEACHER,STUDENT'
        actualScopeType = 'GLOBAL'
      }
      
      const response = await api.post('/message/send', {
        messageType: sendForm.value.messageType,
        scopeType: actualScopeType,
        contentType: sendForm.value.contentType,
        content: sendForm.value.content,
        roleMask: roleMask
      })
      
      if (response.data.code === 200) {
        ElMessage.success('发送成功')
        sendDialogVisible.value = false
        loadData()
      }
    } catch (error) {
      console.error('发送失败', error)
      ElMessage.error(error.response?.data?.message || '发送失败')
    } finally {
      sending.value = false
    }
  })
}

const showMessageDetail = (row) => {
  currentMessage.value = row
  detailVisible.value = true
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const getMessageTypeText = (type) => {
  const typeMap = {
    'INSTANT_MESSAGE': '即时消息',
    'SYSTEM_NOTICE': '系统通知',
    'INTERACTION_REMINDER': '互动提醒',
    'PLATFORM_ANNOUNCEMENT': '平台公告'
  }
  return typeMap[type] || type || '未知'
}

const getMessageTypeTagType = (type) => {
  const typeMap = {
    'INSTANT_MESSAGE': '',
    'SYSTEM_NOTICE': 'success',
    'INTERACTION_REMINDER': 'warning',
    'PLATFORM_ANNOUNCEMENT': 'danger'
  }
  return typeMap[type] || ''
}

const getScopeTypeText = (type) => {
  const typeMap = {
    'PRIVATE': '私聊',
    'COURSE': '课程',
    'GROUP': '群组',
    'GLOBAL': '全局'
  }
  return typeMap[type] || type || '未知'
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
