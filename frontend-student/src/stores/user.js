import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userId = ref(sessionStorage.getItem('userId') || '')
  const studentId = ref(sessionStorage.getItem('studentId') || '')
  const username = ref(sessionStorage.getItem('username') || '')
  const role = ref(sessionStorage.getItem('role') || '')
  const avatar = ref(sessionStorage.getItem('avatar') || '')

  async function loadStudentId() {
    if (role.value !== 'STUDENT' || !userId.value) {
      return false
    }
    try {
      const response = await api.get(`/student/user/${userId.value}`)
      if (response.data.code === 200 && response.data.data) {
        const data = response.data.data
        // 后端返回的是studentId，不是id
        studentId.value = data.studentId
        sessionStorage.setItem('studentId', data.studentId)
        // 加载头像
        if (data.avatarUrl) {
          avatar.value = data.avatarUrl
          sessionStorage.setItem('avatar', data.avatarUrl)
        }
        return true
      }
      return false
    } catch (error) {
      // 404错误说明学生记录不存在，这是正常的
      if (error.response?.status === 404 || error.response?.data?.code === 404) {
        console.log('学生记录不存在，需要完善个人信息')
        return false
      }
      console.error('获取学生信息失败', error)
      return false
    }
  }

  async function setUser(userInfo) {
    token.value = userInfo.token
    userId.value = userInfo.userId
    username.value = userInfo.username
    role.value = userInfo.role
    avatar.value = userInfo.avatar || ''
    
    sessionStorage.setItem('token', userInfo.token)
    sessionStorage.setItem('userId', userInfo.userId)
    sessionStorage.setItem('username', userInfo.username)
    sessionStorage.setItem('role', userInfo.role)
    if (userInfo.avatar) {
      sessionStorage.setItem('avatar', userInfo.avatar)
    }
    
    // 如果是学生，加载学生ID（等待完成）
    if (userInfo.role === 'STUDENT') {
      await loadStudentId()
    }
  }

  function clearUser() {
    token.value = ''
    userId.value = ''
    studentId.value = ''
    username.value = ''
    role.value = ''
    avatar.value = ''
    
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('studentId')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('avatar')
  }

  async function login(loginForm) {
    const response = await api.post('/auth/login', {
      ...loginForm,
      type: 'STUDENT'
    })
    if (response.data.code === 200) {
      await setUser(response.data.data)
      return true
    }
    return false
  }

  return {
    token,
    userId,
    studentId,
    username,
    role,
    avatar,
    setUser,
    clearUser,
    login,
    loadStudentId
  }
})

