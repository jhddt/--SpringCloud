import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userId = ref(sessionStorage.getItem('userId') || '')
  const username = ref(sessionStorage.getItem('username') || '')
  const role = ref(sessionStorage.getItem('role') || '')
  const avatar = ref(sessionStorage.getItem('avatar') || '')

  function setUser(userInfo) {
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
  }

  function clearUser() {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ''
    avatar.value = ''
    
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('avatar')
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

