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

  async function login(loginForm) {
    const response = await api.post('/auth/login', {
      ...loginForm,
      type: 'ADMIN'
    })
    if (response.data.code === 200) {
      setUser(response.data.data)
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
    login
  }
})

