import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
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
        path: 'students',
        name: 'StudentManagement',
        component: () => import('@/views/StudentManagement.vue')
      },
      {
        path: 'teachers',
        name: 'TeacherManagement',
        component: () => import('@/views/TeacherManagement.vue')
      },
      {
        path: 'courses',
        name: 'CourseManagement',
        component: () => import('@/views/CourseManagement.vue')
      },
      {
        path: 'selections',
        name: 'SelectionManagement',
        component: () => import('@/views/SelectionManagement.vue')
      },
      {
        path: 'messages',
        name: 'MessageManagement',
        component: () => import('@/views/MessageManagement.vue')
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
    } else if (to.meta.role && userStore.role !== 'ADMIN') {
      ElMessage.error('无权限访问管理员端')
      next('/login')
    } else {
      next()
    }
  } else {
    if (to.path === '/login' && userStore.token && userStore.role === 'ADMIN') {
      next('/')
    } else {
      next()
    }
  }
})

export default router

