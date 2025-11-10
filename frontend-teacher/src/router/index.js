import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'my-course',
        name: 'MyCourse',
        component: () => import('@/views/MyCourse.vue')
      },
      {
        path: 'selection-management',
        name: 'SelectionManagement',
        component: () => import('@/views/SelectionManagement.vue')
      },
      {
        path: 'grade-management',
        name: 'GradeManagement',
        component: () => import('@/views/GradeManagement.vue')
      },
      {
        path: 'message-center',
        name: 'MessageCenter',
        component: () => import('@/views/MessageCenter.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    if (!userStore.token) {
      next('/login')
    } else if (to.meta.role && userStore.role !== 'TEACHER') {
      ElMessage.error('无权限访问教师端')
      next('/login')
    } else {
      next()
    }
  } else {
    if (to.path === '/login' && userStore.token && userStore.role === 'TEACHER') {
      next('/')
    } else {
      next()
    }
  }
})

export default router

