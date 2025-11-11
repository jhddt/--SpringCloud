<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <div>
            <el-select
              v-model="filterMessageType"
              placeholder="消息类型"
              clearable
              style="width: 150px; margin-right: 10px;"
              @change="handleFilterChange"
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
              @change="handleFilterChange"
            >
              <el-option label="全部" value="" />
              <el-option label="私聊" value="PRIVATE" />
              <el-option label="课程" value="COURSE" />
              <el-option label="群组" value="GROUP" />
              <el-option label="全局" value="GLOBAL" />
            </el-select>
            <el-button type="primary" @click="handleSend">发送消息</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="收到的消息" name="received">
          <el-table :data="receivedMessages" v-loading="loading" border>
            <el-table-column prop="senderName" label="发送者" width="120" />
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
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">
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
    <el-dialog v-model="sendDialogVisible" title="发送消息" width="600px">
      <el-form :model="sendForm" label-width="100px" :rules="sendRules" ref="sendFormRef">
        <el-form-item label="消息类型" prop="messageType" required>
          <el-radio-group v-model="sendForm.messageType" @change="handleMessageTypeChange">
            <el-radio label="INSTANT_MESSAGE">即时消息</el-radio>
            <el-radio label="SYSTEM_NOTICE">系统通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="范围类型" prop="scopeType" required>
          <el-radio-group v-model="sendForm.scopeType" @change="handleScopeTypeChange">
            <el-radio label="PRIVATE">私聊</el-radio>
            <el-radio label="COURSE" :disabled="sendForm.messageType !== 'SYSTEM_NOTICE'">课程</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item 
          v-if="sendForm.scopeType === 'COURSE'" 
          label="选择课程" 
          prop="scopeId" 
          required
        >
          <el-select
            v-model="sendForm.scopeId"
            placeholder="请选择课程"
            style="width: 100%;"
            filterable
          >
            <el-option
              v-for="course in myCourseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item 
          v-if="sendForm.scopeType === 'PRIVATE'" 
          label="接收者" 
          prop="receiverId" 
          required
        >
          <el-select
            v-model="selectedReceiver"
            placeholder="请选择接收者"
            style="width: 100%;"
            filterable
            value-key="userId"
            @change="handleReceiverChange"
          >
            <el-option-group label="学生">
              <el-option
                v-for="student in studentList"
                :key="`student-${student.studentId || student.id}`"
                :label="`${student.realName || student.name} (${student.studentNo || student.username})`"
                :value="{ userId: student.userId, type: 'STUDENT' }"
              />
            </el-option-group>
            <el-option-group label="教师">
              <el-option
                v-for="teacher in teacherList"
                :key="`teacher-${teacher.teacherId || teacher.id}`"
                :label="`${teacher.realName || teacher.name} (${teacher.teacherNo || teacher.username})`"
                :value="{ userId: teacher.userId, type: 'TEACHER' }"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" prop="content" required>
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
        <el-descriptions-item label="状态" v-if="currentMessage.status !== undefined">
          <el-tag :type="currentMessage.status === 1 ? 'success' : 'warning'">
            {{ currentMessage.status === 1 ? '已读' : '未读' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    
    <!-- 全局公告弹窗（可关闭，不重复提醒） -->
    <el-dialog v-model="globalDialogVisible" title="平台公告" width="600px" :close-on-click-modal="false">
      <div v-if="currentAnnouncement" style="white-space: pre-wrap; line-height: 1.8;">
        {{ currentAnnouncement.content }}
      </div>
      <template #footer>
        <el-button @click="skipAnnouncement">稍后提醒</el-button>
        <el-button type="primary" @click="dismissAnnouncement">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- 实时对话框 -->
    <el-dialog v-model="chatVisible" title="实时对话" width="900px" @open="initChat">
      <div class="chat-layout">
        <div class="chat-aside">
          <div class="aside-header">最近联系人</div>
          <el-input v-model="chatSearch" placeholder="搜索联系人" size="small" clearable style="margin: 8px;"/>
          <div class="aside-list">
            <div
              v-for="c in filteredConversations"
              :key="c.key"
              :class="['conv-item', chatPeer && chatPeer.userId === c.userId ? 'active' : '']"
              @click="selectConversation(c)"
            >
              <div class="conv-title">{{ c.name }}</div>
              <div class="conv-sub">{{ c.lastContent }}</div>
            </div>
          </div>
        </div>
        <div class="chat-main">
          <div style="margin-bottom: 10px; display: flex; gap: 8px;">
            <el-select
              v-model="chatPeer"
              placeholder="选择对话对象"
              style="flex: 1;"
              filterable
              value-key="userId"
              @change="onPeerChanged"
            >
              <el-option-group label="学生">
                <el-option
                  v-for="student in studentList"
                  :key="`chat-student-${student.studentId || student.id}`"
                  :label="`${student.realName || student.name} (${student.studentNo || student.username})`"
                  :value="{ userId: student.userId, type: 'STUDENT', name: student.realName || student.name }"
                />
              </el-option-group>
              <el-option-group label="教师">
                <el-option
                  v-for="teacher in teacherList"
                  :key="`chat-teacher-${teacher.teacherId || teacher.id}`"
                  :label="`${teacher.realName || teacher.name} (${teacher.teacherNo || teacher.username})`"
                  :value="{ userId: teacher.userId, type: 'TEACHER', name: teacher.realName || teacher.name }"
                />
              </el-option-group>
            </el-select>
            <el-button @click="refreshConversation" :disabled="!chatPeer">刷新</el-button>
          </div>
          <div class="chat-box">
            <div class="chat-messages" ref="chatScrollRef" @scroll.passive="onChatScroll">
              <div v-if="loadingHistory" class="loading">加载中...</div>
              <div v-for="m in chatMessages" :key="m.messageId || m.localId" :class="['msg', m.isMine ? 'mine' : 'peer']">
                <div class="meta">{{ m.isMine ? '我' : (m.senderName || '对方') }} · {{ formatTime(m.createdAt || m.createTime) }}</div>
                <div class="bubble">{{ m.content }}</div>
              </div>
            </div>
            <div class="chat-input">
              <el-input
                v-model="chatInput"
                type="textarea"
                :rows="3"
                placeholder="输入消息，回车发送"
                @keyup.enter.exact.prevent="sendChat"
              />
              <el-button type="primary" @click="sendChat" :disabled="!chatPeer || !chatInput.trim()" style="margin-left: 8px;">发送</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="chatVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-button type="primary" style="position: fixed; right: 24px; bottom: 24px;" @click="openChat">实时对话</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
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
const sending = ref(false)
const sendForm = ref({
  messageType: 'INSTANT_MESSAGE',
  scopeType: 'PRIVATE',
  scopeId: null,
  receiverId: null,
  receiverType: null,
  contentType: 'TEXT',
  content: ''
})
const sendRules = {
  messageType: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择范围类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }]
}
const sendFormRef = ref(null)
const studentList = ref([])
const teacherList = ref([])
const myCourseList = ref([])
const detailVisible = ref(false)
const currentMessage = ref(null)
const filterMessageType = ref('')
const filterScopeType = ref('')
const selectedReceiver = ref(null)

// 全局公告弹窗相关
const globalDialogVisible = ref(false)
const currentAnnouncement = ref(null)
const announcementQueue = ref([])
const DISMISSED_KEY = 'dismissedGlobalAnnouncements'

const getDismissedSet = () => {
  try {
    const raw = localStorage.getItem(DISMISSED_KEY)
    if (!raw) return new Set()
    const arr = JSON.parse(raw)
    return new Set(Array.isArray(arr) ? arr : [])
  } catch {
    return new Set()
  }
}

const setDismissed = (ids) => {
  try {
    localStorage.setItem(DISMISSED_KEY, JSON.stringify(Array.from(ids)))
  } catch {}
}

const loadReceivedMessages = async () => {
  loading.value = true
  try {
    const params = {
      current: receivedPage.value,
      size: pageSize.value
    }
    
    // 添加过滤条件
    if (filterMessageType.value) {
      params.messageType = filterMessageType.value
    }
    if (filterScopeType.value) {
      params.scopeType = filterScopeType.value
    }
    
    const response = await api.get('/message/page', { params })
    
    if (response.data.code === 200) {
      const records = response.data.data.records || []
      // 显示所有消息，包括平台公告（后端已通过 roleMask 控制可见性）
      receivedMessages.value = records
      receivedTotal.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载收到的消息失败', error)
    ElMessage.error(error.response?.data?.message || '加载消息失败')
  } finally {
    loading.value = false
  }
}

const loadSentMessages = async () => {
  loading.value = true
  try {
    const params = {
      current: sentPage.value,
      size: pageSize.value
    }
    
    // 添加过滤条件
    if (filterMessageType.value) {
      params.messageType = filterMessageType.value
    }
    if (filterScopeType.value) {
      params.scopeType = filterScopeType.value
    }
    
    const response = await api.get('/message/page', { params })
    
    if (response.data.code === 200) {
      const records = response.data.data.records || []
      // 发送的消息中也不展示平台公告
      sentMessages.value = records.filter(r => !(r.scopeType === 'GLOBAL' || r.messageType === 'PLATFORM_ANNOUNCEMENT'))
      sentTotal.value = sentMessages.value.length
    }
  } catch (error) {
    console.error('加载发送的消息失败', error)
    ElMessage.error(error.response?.data?.message || '加载消息失败')
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

const loadMyCourses = async () => {
  try {
    const response = await api.get('/course/page', {
      params: { current: 1, size: 1000, teacherId: userStore.userId }
    })
    if (response.data.code === 200) {
      myCourseList.value = response.data.data.records || []
    }
  } catch (error) {
    console.error('加载课程列表失败', error)
  }
}

const handleTabChange = (name) => {
  if (name === 'received') {
    loadReceivedMessages()
  } else if (name === 'sent') {
    loadSentMessages()
  }
}

const handleFilterChange = () => {
  if (activeTab.value === 'received') {
    loadReceivedMessages()
  } else if (activeTab.value === 'sent') {
    loadSentMessages()
  }
}

const handleSend = () => {
  sendForm.value = {
    messageType: 'INSTANT_MESSAGE',
    scopeType: 'PRIVATE',
    scopeId: null,
    receiverId: null,
    receiverType: null,
    contentType: 'TEXT',
    content: ''
  }
  selectedReceiver.value = null
  sendDialogVisible.value = true
}

const handleMessageTypeChange = () => {
  // 如果选择系统通知，默认范围类型为课程
  if (sendForm.value.messageType === 'SYSTEM_NOTICE') {
    sendForm.value.scopeType = 'COURSE'
    sendForm.value.receiverId = null
    sendForm.value.receiverType = null
  } else {
    sendForm.value.scopeType = 'PRIVATE'
    sendForm.value.scopeId = null
  }
}

const handleScopeTypeChange = () => {
  if (sendForm.value.scopeType === 'PRIVATE') {
    sendForm.value.scopeId = null
  } else if (sendForm.value.scopeType === 'COURSE') {
    sendForm.value.receiverId = null
    sendForm.value.receiverType = null
  }
}

const handleReceiverChange = (value) => {
  // 设置接收者ID和类型（value是对象 { userId, type }）
  if (value && value.userId) {
    sendForm.value.receiverId = value.userId
    sendForm.value.receiverType = value.type
  } else {
    sendForm.value.receiverId = null
    sendForm.value.receiverType = null
  }
}

const handleSubmitSend = async () => {
  if (!sendFormRef.value) return
  
  await sendFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    
    // 验证逻辑
    if (sendForm.value.scopeType === 'PRIVATE' && !sendForm.value.receiverId) {
      ElMessage.warning('请选择接收者')
      return
    }
    if (sendForm.value.scopeType === 'COURSE' && !sendForm.value.scopeId) {
      ElMessage.warning('请选择课程')
      return
    }
    if (!sendForm.value.content) {
      ElMessage.warning('请输入消息内容')
      return
    }
    
    sending.value = true
    try {
      // 构建发送数据
      const sendData = {
        messageType: sendForm.value.messageType,
        scopeType: sendForm.value.scopeType,
        contentType: sendForm.value.contentType,
        content: sendForm.value.content
      }
      
      // 根据范围类型设置不同的字段
      if (sendForm.value.scopeType === 'PRIVATE') {
        // 私聊消息：需要receiverId和receiverType
        sendData.receiverId = sendForm.value.receiverId
        sendData.receiverType = sendForm.value.receiverType
      } else if (sendForm.value.scopeType === 'COURSE') {
        // 课程消息（系统通知）：只需要scopeId
        sendData.scopeId = sendForm.value.scopeId
        // 系统通知不需要receiverId，会发送给课程的所有成员
      }
      
      // 使用新的API接口
      const response = await api.post('/message/send', sendData)
      
      if (response.data.code === 200) {
        ElMessage.success('发送成功')
        sendDialogVisible.value = false
        if (activeTab.value === 'sent') {
          loadSentMessages()
        }
      }
    } catch (error) {
      console.error('发送失败', error)
      ElMessage.error(error.response?.data?.message || '发送失败')
    } finally {
      sending.value = false
    }
  })
}

const handleMarkAsRead = async (row) => {
  try {
    // 新接口：不需要传递userId参数，从请求头获取
    const response = await api.put(`/message/${row.messageId || row.id}/read`)
    if (response.data.code === 200) {
      ElMessage.success('标记成功')
      row.status = 1
    }
  } catch (error) {
    console.error('标记已读失败', error)
    ElMessage.error(error.response?.data?.message || '标记失败')
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

// 加载全局公告并弹窗展示（仅展示未关闭过的）
const loadGlobalAnnouncements = async () => {
  try {
    const params = {
      current: 1,
      size: 5,
      scopeType: 'GLOBAL',
      messageType: 'PLATFORM_ANNOUNCEMENT'
    }
    const response = await api.get('/message/page', { params })
    if (response.data.code === 200) {
      const records = response.data.data.records || []
      const dismissed = getDismissedSet()
      const queue = records.filter(r => !dismissed.has(r.messageId || r.id))
      if (queue.length > 0) {
        announcementQueue.value = queue
        showNextAnnouncement()
      }
    }
  } catch {}
}

const showNextAnnouncement = () => {
  if (!announcementQueue.value || announcementQueue.value.length === 0) {
    currentAnnouncement.value = null
    globalDialogVisible.value = false
    return
  }
  currentAnnouncement.value = announcementQueue.value.shift()
  globalDialogVisible.value = true
}

const dismissAnnouncement = () => {
  if (!currentAnnouncement.value) return
  const id = currentAnnouncement.value.messageId || currentAnnouncement.value.id
  const dismissed = getDismissedSet()
  dismissed.add(id)
  setDismissed(dismissed)
  showNextAnnouncement()
}

const skipAnnouncement = () => {
  globalDialogVisible.value = false
  currentAnnouncement.value = null
}

// ===== 实时对话 =====
const chatVisible = ref(false)
const chatPeer = ref(null) // { userId, type, name }
const chatMessages = ref([])
const chatInput = ref('')
const chatScrollRef = ref(null)
let stompClient = null

const chatConversations = ref([])
const chatSearch = ref('')
const filteredConversations = computed(() => {
  if (!chatSearch.value) return chatConversations.value
  const k = chatSearch.value.toLowerCase()
  return chatConversations.value.filter(c => (c.name || '').toLowerCase().includes(k) || (c.lastContent || '').toLowerCase().includes(k))
})

const chatPage = ref(1)
const chatPageSize = 20
const chatHasMore = ref(true)
const loadingHistory = ref(false)

const buildConversations = (records) => {
  const map = new Map()
  for (const m of records) {
    const otherId = m.senderId === userStore.userId ? m.receiverId : m.senderId
    const otherType = m.senderId === userStore.userId ? m.receiverType : m.senderType
    const name = m.senderId === userStore.userId ? (m.receiverName || `用户${otherId}`) : (m.senderName || `用户${otherId}`)
    const key = `${otherType}-${otherId}`
    const exist = map.get(key)
    const time = new Date(m.createdAt || m.createTime || Date.now()).getTime()
    if (!exist || time > exist.lastTime) {
      map.set(key, { key, userId: otherId, type: otherType, name, lastTime: time, lastContent: m.content })
    }
  }
  chatConversations.value = Array.from(map.values()).sort((a,b)=>b.lastTime - a.lastTime)
}

const loadRecentConversations = async () => {
  try {
    const params = { current: 1, size: 100, scopeType: 'PRIVATE' }
    const res = await api.get('/message/page', { params })
    if (res.data.code === 200) {
      buildConversations(res.data.data.records || [])
    }
  } catch {}
}

const onPeerChanged = () => {
  loadConversation(true)
}

const selectConversation = (c) => {
  chatPeer.value = { userId: c.userId, type: c.type, name: c.name }
  loadConversation(true)
}

const onChatScroll = async () => {
  const el = chatScrollRef.value
  if (!el || loadingHistory.value || !chatHasMore.value) return
  if (el.scrollTop <= 0) {
    await loadOlder()
  }
}

const loadConversation = async (reset = false) => {
  if (!chatPeer.value) return
  if (reset) {
    chatPage.value = 1
    chatHasMore.value = true
    chatMessages.value = []
  }
  await loadOlder(true)
}

const loadOlder = async (initial = false) => {
  if (!chatPeer.value || !chatHasMore.value) return
  try {
    loadingHistory.value = true
    const beforeHeight = chatScrollRef.value ? chatScrollRef.value.scrollHeight : 0
    const res = await api.get('/message/list', { params: { otherUserId: chatPeer.value.userId, current: chatPage.value, size: chatPageSize } })
    if (res.data.code === 200) {
      const list = res.data.data.records || []
      const mapped = list.map(m => ({ ...m, isMine: m.senderId === userStore.userId }))
      chatMessages.value = [...mapped.reverse(), ...chatMessages.value]
      chatHasMore.value = list.length === chatPageSize
      chatPage.value += 1
      if (chatScrollRef.value) {
        if (initial) {
          chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
        } else {
          const after = chatScrollRef.value.scrollHeight
          chatScrollRef.value.scrollTop = after - beforeHeight
        }
      }
    }
  } catch (e) {
    console.error('加载历史失败', e)
  } finally {
    loadingHistory.value = false
  }
}

const refreshConversation = () => loadConversation(true)

const loadStompCdn = async () => {
  const ensure = (src) => new Promise((resolve) => {
    if ([...document.scripts].some(s => s.src.includes(src))) return resolve()
    const s = document.createElement('script')
    s.src = src
    s.onload = () => resolve()
    document.body.appendChild(s)
  })
  await ensure('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js')
  await ensure('https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js')
}

const initChat = async () => {
  try {
    await loadStompCdn()
    if (stompClient && stompClient.connected) return
    const endpoint = '/api/message/ws'
    const socket = new window.SockJS(endpoint)
    stompClient = window.Stomp.over(socket)
    stompClient.debug = null
    stompClient.connect({}, () => {
      stompClient.subscribe('/user/queue/messages', (msg) => {
        try {
          const data = JSON.parse(msg.body)
          buildConversations([data, ...chatMessages.value])
          if (chatPeer.value && data.senderId === chatPeer.value.userId) {
            chatMessages.value.push({ ...data, isMine: false })
            if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
          }
        } catch (e) {}
      })
    })
  } catch (e) {
    console.error('初始化聊天失败', e)
  }
}

const openChat = () => {
  chatVisible.value = true
  if (!chatPeer.value) {
    if (chatConversations.value.length > 0) {
      const c = chatConversations.value[0]
      chatPeer.value = { userId: c.userId, type: c.type, name: c.name }
    } else if (studentList.value.length > 0) {
      chatPeer.value = { userId: studentList.value[0].userId, type: 'STUDENT', name: studentList.value[0].realName || studentList.value[0].name }
    } else if (teacherList.value.length > 0) {
      chatPeer.value = { userId: teacherList.value[0].userId, type: 'TEACHER', name: teacherList.value[0].realName || teacherList.value[0].name }
    }
  }
  if (chatPeer.value) loadConversation(true)
}

const sendChat = async () => {
  if (!chatPeer.value || !chatInput.value.trim()) return
  const content = chatInput.value.trim()
  try {
    const payload = {
      messageType: 'INSTANT_MESSAGE',
      scopeType: 'PRIVATE',
      contentType: 'TEXT',
      content,
      receiverId: chatPeer.value.userId,
      receiverType: chatPeer.value.type
    }
    const res = await api.post('/message/send', payload)
    if (res.data.code === 200) {
      const saved = res.data.data || {}
      chatMessages.value.push({ ...saved, isMine: true })
      chatInput.value = ''
      if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  } catch (e) {
    console.error('发送失败', e)
    ElMessage.error(e.response?.data?.message || '发送失败')
  }
}

onMounted(() => {
  loadReceivedMessages()
  loadStudentList()
  loadTeacherList()
  loadMyCourses()
  loadGlobalAnnouncements()
  loadRecentConversations()
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

.chat-box { display: flex; flex-direction: column; height: 400px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 8px; background: #f5f7fa; border: 1px solid #ebeef5; border-radius: 4px; }
.msg { margin: 8px 0; max-width: 70%; }
.msg.mine { margin-left: auto; text-align: right; }
.msg .meta { font-size: 12px; color: #909399; margin-bottom: 4px; }
.msg .bubble { display: inline-block; padding: 8px 10px; border-radius: 6px; background: #fff; border: 1px solid #ebeef5; }
.msg.mine .bubble { background: #409EFF; color: #fff; border-color: #409EFF; }
.chat-input { display: flex; margin-top: 8px; align-items: flex-end; }

.chat-layout { display: flex; gap: 12px; }
.chat-aside { width: 260px; border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; background: #fff; display: flex; flex-direction: column; }
.aside-header { font-weight: 600; padding: 10px 12px; border-bottom: 1px solid #ebeef5; }
.aside-list { flex: 1; overflow-y: auto; }
.conv-item { padding: 10px 12px; border-bottom: 1px solid #f2f2f2; cursor: pointer; }
.conv-item.active { background: #ecf5ff; }
.conv-title { font-size: 14px; color: #303133; }
.conv-sub { font-size: 12px; color: #909399; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chat-main { flex: 1; }
</style>
