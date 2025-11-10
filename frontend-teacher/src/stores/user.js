import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  function setUser(userInfo) {
    token.value = userInfo.token
    userId.value = userInfo.userId
    username.value = userInfo.username
    role.value = userInfo.role
    avatar.value = userInfo.avatar || ''
    
    localStorage.setItem('token', userInfo.token)
    localStorage.setItem('userId', userInfo.userId)
    localStorage.setItem('username', userInfo.username)
    localStorage.setItem('role', userInfo.role)
    if (userInfo.avatar) {
      localStorage.setItem('avatar', userInfo.avatar)
    }
  }

  function clearUser() {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ''
    avatar.value = ''
    
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('avatar')
  }

  async function loadTeacherInfo() {
    if (role.value !== 'TEACHER' || !userId.value) {
      return false
    }
    try {
      // 通过userId获取教师信息（类似于学生端的getByUserId）
      const response = await api.get(`/teacher/user/${userId.value}`)
      if (response.data.code === 200 && response.data.data) {
        const data = response.data.data
        // 加载头像
        if (data.avatarUrl) {
          avatar.value = data.avatarUrl
          localStorage.setItem('avatar', data.avatarUrl)
        }
        return true
      }
      return false
    } catch (error) {
      // 404错误说明教师记录不存在，这是正常的
      if (error.response?.status === 404 || error.response?.data?.code === 404) {
        console.log('教师记录不存在，需要完善个人信息')
        return false
      }
      console.error('获取教师信息失败', error)
      return false
    }
  }

  async function login(loginForm) {
    const response = await api.post('/auth/login', {
      ...loginForm,
      type: 'TEACHER'
    })
    if (response.data.code === 200) {
      await setUser(response.data.data)
      // 登录后加载教师信息（包括头像）
      await loadTeacherInfo()
      return true
    }
    return false
  }

  return {
    token,
    userId,
    username,
    role,
    avatar,
    setUser,
    clearUser,
    login,
    loadTeacherInfo
  }
})

