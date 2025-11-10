<template>
  <el-container class="layout-container">
    <el-aside width="260px" class="sidebar">
      <div class="logo-container">
        <div class="logo-icon">
          <el-icon :size="28"><User /></el-icon>
        </div>
        <div class="logo-text">
          <div class="logo-title">学生端</div>
          <div class="logo-subtitle">Student Portal</div>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard" class="menu-item">
          <el-icon><House /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/course-browse" class="menu-item">
          <el-icon><Document /></el-icon>
          <span>课程浏览</span>
        </el-menu-item>
        <el-menu-item index="/my-selection" class="menu-item">
          <el-icon><List /></el-icon>
          <span>我的选课</span>
        </el-menu-item>
        <el-menu-item index="/grade-query" class="menu-item">
          <el-icon><Trophy /></el-icon>
          <span>成绩查询</span>
        </el-menu-item>
        <el-menu-item index="/message-center" class="menu-item">
          <el-icon><Message /></el-icon>
          <span>消息中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" class="user-dropdown">
            <div class="user-info">
              <div class="avatar-wrapper">
                <img 
                  v-if="userStore.avatar && !avatarError" 
                  :src="userStore.avatar" 
                  alt="头像" 
                  class="user-avatar"
                  @error="handleAvatarError"
                />
                <el-icon v-else class="user-icon"><UserFilled /></el-icon>
              </div>
              <span class="username">{{ userStore.username }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  <span>个人信息</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { House, Document, List, Trophy, Message, User, UserFilled, SwitchButton, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const avatarError = ref(false)

const activeMenu = computed(() => route.path)

// 监听头像变化，重置错误状态
watch(() => userStore.avatar, (newAvatar) => {
  avatarError.value = false
}, { immediate: true })

// 组件挂载时，如果没有头像，尝试加载
onMounted(async () => {
  // 如果用户已登录但没有头像，尝试加载
  if (userStore.token && userStore.role === 'STUDENT' && !userStore.avatar) {
    await userStore.loadStudentId()
  }
})

const pageTitle = computed(() => {
  const titleMap = {
    '/dashboard': '仪表盘',
    '/course-browse': '课程浏览',
    '/my-selection': '我的选课',
    '/grade-query': '成绩查询',
    '/message-center': '消息中心',
    '/profile': '个人信息'
  }
  return titleMap[route.path] || '仪表盘'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.clearUser()
    router.push('/login')
  })
}

const handleAvatarError = () => {
  // 头像加载失败时，标记错误，显示默认图标
  avatarError.value = true
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: var(--bg-secondary);
}

.sidebar {
  background: var(--bg-sidebar);
  box-shadow: 4px 0 12px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.1) 0%, transparent 100%);
  pointer-events: none;
}

.logo-container {
  display: flex;
  align-items: center;
  padding: 20px;
  height: 80px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(16, 185, 129, 0.1);
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  border-radius: 12px;
  margin-right: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.logo-text {
  flex: 1;
}

.logo-title {
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.logo-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.sidebar-menu {
  border: none;
  padding: 20px 0;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 8px 12px;
  border-radius: 12px;
  height: 48px;
  line-height: 48px;
  transition: all var(--transition-base);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  transform: translateX(4px);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  display: none;
}

.sidebar-menu :deep(.el-icon) {
  margin-right: 12px;
  font-size: 20px;
}

.header {
  background: linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
  transition: all var(--transition-base);
}

.user-info:hover {
  background: #dcfce7;
}

.avatar-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-icon {
  color: var(--primary-color);
  font-size: 20px;
}

.username {
  color: var(--text-primary);
  font-weight: 500;
  font-size: 14px;
}

.dropdown-icon {
  color: var(--text-secondary);
  font-size: 14px;
  transition: transform var(--transition-base);
}

.user-dropdown:hover .dropdown-icon {
  transform: rotate(180deg);
}

.user-dropdown :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.main-content {
  background: var(--bg-secondary);
  padding: 30px;
  overflow-y: auto;
}

.main-content :deep(.el-card) {
  border-radius: 16px;
  border: none;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-base);
}

.main-content :deep(.el-card:hover) {
  box-shadow: var(--shadow-lg);
}
</style>

